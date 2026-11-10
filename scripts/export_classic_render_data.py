"""Pack original x2 tile files and recover the supplied Windows client's poses.

The pose extraction takes script.json from Perfare/Il2CppDumper. It reads
GameAssembly.dll via objdump and never executes the game.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import subprocess

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'web/classic/assets'
CELL = 48
COLUMNS = 8


def export_tiles():
    source = ROOT / 'data/res/x2'
    sets = {}
    for file in source.iterdir():
        match = re.fullmatch(r'(\d+)\$(\d+)', file.name)
        if match:
            set_id, tile_id = map(int, match.groups())
            sets.setdefault(set_id, {})[tile_id] = file
    if set(sets) != set(range(1, 34)):
        raise ValueError('Original resource directory must contain all 33 tilesets')

    output = ASSETS / 'tiles'
    output.mkdir(parents=True, exist_ok=True)
    manifest = {}
    for set_id, tiles in sorted(sets.items()):
        # A few later maps use an extra tile present in the original x1 strip.
        strip_file = ROOT / 'data/res/x1' / str(set_id)
        strip = Image.open(strip_file).convert('RGBA') if strip_file.is_file() else None
        count = max(max(tiles), strip.height // 24 if strip else 0)
        atlas = Image.new('RGBA', (COLUMNS * CELL, ((count + COLUMNS - 1) // COLUMNS) * CELL))
        for tile_id in range(1, count + 1):
            if tile_id in tiles:
                with Image.open(tiles[tile_id]) as tile:
                    image = tile.convert('RGBA')
            elif strip and tile_id * 24 <= strip.height:
                y = (tile_id - 1) * 24
                image = strip.crop((0, y, 24, y + 24)).resize((CELL, CELL), Image.Resampling.NEAREST)
            else:
                raise ValueError(f'Missing tile {set_id}${tile_id}')
            if image.width != CELL or image.height not in (2, CELL):
                raise ValueError(f'Invalid tile dimensions {set_id}${tile_id}: {image.size}')
            i = tile_id - 1
            atlas.paste(image, (i % COLUMNS * CELL, i // COLUMNS * CELL))
        atlas.save(output / f'{set_id}.png', optimize=True)
        manifest[set_id] = {'count': count, 'flags': {}}

    # The original client skips invisible collision cells (flag 256).
    data = (ROOT / 'data/map/tile_set_info').read_bytes()
    offset = 1
    for set_id in range(1, data[0] + 1):
        groups = data[offset]
        offset += 1
        for _ in range(groups):
            flag, count = struct.unpack_from('>IB', data, offset)
            offset += 5
            for tile_id in data[offset:offset + count]:
                if set_id in manifest:
                    flags = manifest[set_id]['flags']
                    flags[tile_id] = flags.get(tile_id, 0) | flag
            offset += count
    if offset != len(data):
        raise ValueError('Unexpected trailing tile classification data')
    (output / 'manifest.json').write_text(json.dumps(manifest, separators=(',', ':')) + '\n', encoding='utf-8')


def export_poses(dump):
    tables = json.loads(dump.read_text(encoding='utf-8'))
    methods = {m['Name']: m['Address'] for m in tables['ScriptMethod']}
    fields = {m['Address']: m['Name'].split('.')[-1].upper()
              for m in tables['ScriptMetadata']
              if m['Name'].startswith('Field$<PrivateImplementationDetails>.')}

    client = ROOT / 'Mod_Local'
    metadata = (client / 'srcnrofree.online_Data/il2cpp_data/Metadata/global-metadata.dat').read_bytes()
    magic, version = struct.unpack_from('<II', metadata)
    if (magic, version) != (0xFAB11BAF, 31):
        raise ValueError('Expected the supplied IL2CPP v31 client')
    fields_offset, fields_length = struct.unpack_from('<II', metadata, 8 + 7 * 8)
    data_offset, data_length = struct.unpack_from('<II', metadata, 8 + 8 * 8)
    vectors = {}
    for _, _, offset in struct.iter_unpack('<iii', metadata[fields_offset:fields_offset + fields_length]):
        if 0 <= offset <= data_length - 12:
            data = metadata[data_offset + offset:data_offset + offset + 12]
            vectors[hashlib.sha256(data).hexdigest().upper()] = list(struct.unpack('<iii', data))

    # Game1.Char static constructor builds CharInfo[33][4][3]. Offsets
    # 0x20, 0x28, ... are IL2CPP array elements. Each triplet contains
    # sprite frame, x offset and height above the baseline.
    start = methods['Game1.Char$$.cctor']
    end = min(address for address in tables['Addresses'] if address > start)
    image_base = 0x180000000
    assembly = subprocess.check_output([
        'objdump', '-d', '-Mintel', '--no-show-raw-insn',
        f'--start-address={image_base + start}', f'--stop-address={image_base + end}',
        str(client / 'GameAssembly.dll')
    ], text=True)
    types = {m['Name']: m['Address'] for m in tables['ScriptMetadata']}
    int_array, nested = types['int[]_TypeInfo'], types['int[][]_TypeInfo']
    start_pattern = fr'mov\s+rcx,QWORD PTR \[rip[^\n]+# 0x{image_base + nested:x}\s*\n'
    chunks = re.split(start_pattern, assembly.split('QWORD PTR [rcx+0x90],rbx', 1)[0])
    poses = []
    for chunk in chunks[1:]:
        if not re.match(r'\s*[0-9a-f]+:\s+mov\s+edx,0x4\b', chunk):
            continue
        vectors_in_frame = re.split(fr'mov\s+rcx,QWORD PTR \[rip[^\n]+# 0x{image_base + int_array:x}\s*\n', chunk)
        if len(vectors_in_frame) != 5:
            continue
        pose = []
        for part in vectors_in_frame[1:]:
            constant = re.search(r'mov\s+rdx,QWORD PTR \[rip[^\n]+# 0x([a-f0-9]+)', part)
            if constant and int(constant[1], 16) - image_base in fields:
                pose.append(vectors[fields[int(constant[1], 16) - image_base]])
                continue
            values = [0, 0, 0]
            for byte_offset, value in re.findall(r'mov\s+DWORD PTR \[rax\+0x(20|24|28)\],0x([a-f0-9]+)', part):
                number = int(value, 16)
                values[(int(byte_offset, 16) - 32) // 4] = number - (1 << 32) if number >= 1 << 31 else number
            pose.append(values)
        poses.append(pose)
    if len(poses) != 33 or poses[0][:3] != [[0, -13, 34], [1, -8, 10], [1, -9, 16]]:
        raise ValueError('Could not verify the original CharInfo table')
    (ASSETS / 'character-poses.json').write_text(json.dumps(poses, separators=(',', ':')) + '\n', encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--il2cpp-dump', type=Path, help='Il2CppDumper script.json')
    args = parser.parse_args()
    export_tiles()
    if args.il2cpp_dump:
        export_poses(args.il2cpp_dump)


if __name__ == '__main__':
    main()
