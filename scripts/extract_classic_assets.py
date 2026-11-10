"""Extract original low-resolution client artwork from the supplied Windows build.

Development-only dependency: UnityPy. The exported files are regular PNG/bin assets;
the Render image does not need Unity or UnityPy.
"""
from pathlib import Path
import sys,json,struct

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.local/unity-tools'))
import UnityPy

SOURCE=ROOT/'Mod_Local/srcnrofree.online_Data'
OUT=ROOT/'web/classic/assets'
OUT.mkdir(parents=True,exist_ok=True)
# Keep the web bundle limited to artwork and metrics that the classic UI uses.
UI_IMAGES = {
    'mytexture2dpanel', 'mytexture2dstat', 'mytexture2dhp', 'mytexture2dmp',
    'mytexture2dskill', 'mytexture2dskill2', 'mytexture2dchat', 'mytexture2dbtx',
    'mytexture2dimgmoney', 'mytexture2dimgdiamond', 'mytexture2dpea0',
    'mytexture2dfirebtn0', 'mytexture2dbong',
    'mytexture2dbtmenu1', 'mytexture2dbtmenu2', 'mytexture2dhatdau', 'mytexture2darrow3',
}
ASSETS = {'res/logo/logo2', 'res/myfont/tahoma_7'}
ASSETS.update('res/x1/mainimage/' + name for name in UI_IMAGES)
ASSETS.update('res/x1/myfont/tahoma_7_' + color for color in ('white', 'yellow', 'red'))

manager=UnityPy.load(str(SOURCE/'globalgamemanagers'))
paths={}
for obj in manager.objects:
    if obj.type.name=='ResourceManager':
        for path,ref in obj.parse_as_dict()['m_Container']:
            if path in ASSETS:
                paths.setdefault(ref['m_PathID'],[]).append(path)
env=UnityPy.load(str(SOURCE/'resources.assets'))
manifest={}
for obj in env.objects:
    if obj.path_id not in paths or obj.type.name not in ('Texture2D','TextAsset'):continue
    data=obj.parse_as_object()
    for resource in paths[obj.path_id]:
        relative=resource.removeprefix('res/')
        target=(OUT/relative).resolve()
        if not target.is_relative_to(OUT.resolve()):raise ValueError('Invalid resource path')
        target.parent.mkdir(parents=True,exist_ok=True)
        if obj.type.name=='Texture2D':
            if data.m_Width<=0 or data.m_Height<=0:continue
            target=target.with_suffix('.png')
            data.image.save(target)
            manifest[relative]={'url':'/classic/assets/'+relative+'.png','width':data.m_Width,'height':data.m_Height}
        else:
            payload=data.m_Script.encode('utf-8',errors='surrogateescape') if isinstance(data.m_Script,str) else data.m_Script
            target.with_suffix('.bin').write_bytes(payload)
            manifest[relative]={'url':'/classic/assets/'+relative+'.bin','bytes':len(payload)}
(OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
print('Exported',len(manifest),'original resource files')

# Original bitmap-font character ordering is a string literal in IL2CPP metadata.
meta=(SOURCE/'il2cpp_data/Metadata/global-metadata.dat').read_bytes()
magic,version,literals,length,data_offset,data_length=struct.unpack_from('<6I',meta)
font_strings=[]
if magic==0xFAB11BAF and literals+length<=len(meta):
    for off in range(literals,literals+length,8):
        n,index=struct.unpack_from('<2I',meta,off)
        if n>150 and n<4000 and data_offset+index+n<=len(meta):
            value=meta[data_offset+index:data_offset+index+n].decode('utf-8',errors='replace')
            if all(c in value for c in '0123456789') and ('ạ' in value or 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' in value):font_strings.append(value)
(OUT/'font-characters.json').write_text(json.dumps(font_strings,ensure_ascii=False),encoding='utf-8')
print('Bitmap character maps found:',len(font_strings))

# Export metrics together with the original Unicode ordering for the browser.
font_data = (OUT / 'myfont/tahoma_7.bin').read_bytes()
characters = font_strings[0]
glyph_count = struct.unpack_from('>H', font_data)[0]
if glyph_count != len(characters):
    raise ValueError('Bitmap font character count does not match metrics')
glyphs = {}
for index, character in enumerate(characters):
    x, y, width, height = struct.unpack_from('>4H', font_data, 2 + index * 8)
    # The source metrics describe a single column. Unity stores two columns.
    glyphs[character] = [x + 10 * (y // 1233), y % 1233, width, height]
(OUT / 'glyphs.json').write_text(json.dumps(glyphs, ensure_ascii=False), encoding='utf-8')
