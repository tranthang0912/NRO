"""Create a source/runtime deployment archive with an explicit allowlist, without secrets."""
from pathlib import Path
import zipfile, hashlib
ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'release' / 'NRO-Web-Render.zip'
TARGET.parent.mkdir(exist_ok=True)
files = []
for name in ('pom.xml','Dockerfile','.dockerignore','.gitignore','render.yaml','Config.properties','.env.example','README.md','VERIFICATION.md','run.bat'):
    p=ROOT/name
    if p.exists(): files.append(p)
for directory,pattern in (('src','*.java'),('test','*.java'),('db','*.sql'),('web','*'),('config','ca.pem'),('scripts','*.py'),('scripts','*.java'),('verification','*.jpg')):
    files.extend(p for p in (ROOT/directory).rglob(pattern) if p.is_file())
for directory in ('map','update_data','icon/x1','res/x1','item_bg_temp/x1','mob/x1','web-mob','effdata'):
    files.extend(p for p in (ROOT/'data'/directory).rglob('*') if p.is_file())
with zipfile.ZipFile(TARGET,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in sorted(set(files)):
        relative=p.relative_to(ROOT).as_posix()
        if relative=='.env' or relative.startswith(('.local/','release/')): raise ValueError('Private file in manifest')
        z.write(p,'NRO-Web-Render/'+relative)
    jar=ROOT/'target/nro-web.jar'
    if jar.exists(): z.write(jar,'NRO-Web-Render/nro-web.jar')
digest=hashlib.sha256(TARGET.read_bytes()).hexdigest()
TARGET.with_suffix('.zip.sha256').write_text(f'{digest}  {TARGET.name}\n',encoding='ascii')
with zipfile.ZipFile(TARGET) as z:
    bad=z.testzip()
    if bad: raise ValueError(f'Corrupt archive member: {bad}')
    names=z.namelist()
    if any(n.endswith('/.env') or n.endswith('/b.sql') or '/Mod_Local/' in n or n.endswith('/src/nro.sql') for n in names): raise ValueError('Legacy/private payload in archive')
print(f'Packaged {len(files)} files; {TARGET.stat().st_size/1024/1024:.1f} MiB; SHA256 {digest}')
