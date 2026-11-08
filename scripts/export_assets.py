"""Export existing mob sprites and frame metadata; no new artwork or image dependencies."""
from pathlib import Path
import struct, json
ROOT = Path(__file__).resolve().parents[1]
out = ROOT / 'data/web-mob'
out.mkdir(exist_ok=True)
frames = {}
for source in (ROOT / 'data/mob/x1').iterdir():
    if not source.is_file() or not source.name.isdigit(): continue
    data = source.read_bytes()
    png = data.find(b'\x89PNG\r\n\x1a\n')
    end = data.find(b'IEND', png)
    if png < 0 or end < 0: continue
    (out / (source.name + '.png')).write_bytes(data[png:end+8])
    try:
        pos = 5
        def u8():
            global pos
            value = data[pos]; pos += 1; return value
        def i16():
            global pos
            value = struct.unpack_from('>h', data, pos)[0]; pos += 2; return value
        pieces = [{key: u8() for key in ('id', 'x', 'y', 'w', 'h')} for _ in range(u8())]
        count = i16()
        if not 0 < count < 1000: continue
        animation = [[{'x': i16(), 'y': i16(), 'piece': u8()} for _ in range(u8())] for _ in range(count)]
        if pos > png: continue
        frames[source.name] = {'pieces': pieces, 'frames': animation}
    except (IndexError, struct.error):
        pass
(out / 'frames.json').write_text(json.dumps(frames, separators=(',', ':')), encoding='utf-8')
print(f'Exported {len(list(out.glob("*.png")))} sprites; {len(frames)} original frame sets.')
