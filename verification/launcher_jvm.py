"""Resolve required JVM arguments from the exact Mojang version metadata.

The launcher must preserve standalone arguments as well as rule-controlled ones.
Heap/user tuning belongs to arguments.default-user-jvm and is intentionally left
to the caller. No installed launcher profile is edited.
"""
import json,re
from pathlib import Path

def resolve(metadata,variables,*,os_name='windows',os_arch='x86_64',os_version='10.0'):
 result=[]
 def matches(rule):
  os=rule.get('os',{})
  if os.get('name',os_name)!=os_name:return False
  if 'arch' in os and not re.fullmatch(os['arch'],os_arch):return False
  if 'version' in os and not re.search(os['version'],os_version):return False
  if 'features' in rule:raise ValueError('JVM feature rules require an explicit feature context')
  return True
 for entry in metadata['arguments']['jvm']:
  if isinstance(entry,str):values=[entry]
  else:
   rules=entry.get('rules');allowed=not rules
   for rule in rules or []:
    if matches(rule):allowed=rule['action']=='allow'
   if not allowed:continue
   values=entry['value'];values=[values] if isinstance(values,str) else values
  for value in values:
   value=re.sub(r'\$\{([^}]+)\}',lambda m:variables[m[1]],value)
   result.append(value)
 return result

def merge_argfile(path,metadata,*,shadow_for_26_2=False):
 """Retain caller heap/game args; replace required JVM options from metadata."""
 path=Path(path);lines=path.read_text('utf-8-sig').splitlines()
 args=[json.loads(x) for x in lines]
 main=args.index('net.fabricmc.loader.impl.launch.knot.KnotClient')
 old=args[:main];game=args[main:]
 variables={'classpath':old[old.index('-cp')+1],
  'natives_directory':str(path.parent/'run/natives').replace('\\','/'),
  'launcher_name':'OpenYSM-isolated-verification','launcher_version':'1'}
 required=resolve(metadata,variables)
 preserve=[];skip=False
 for item in old:
  if skip:skip=False;continue
  if item in ('-cp','--add-exports'):skip=True;continue
  if any(item.startswith(prefix) for prefix in ('-Djava.library.path=','-Djna.tmpdir=','-Dorg.lwjgl.system.SharedLibraryExtractPath=','-Dio.netty.native.workdir=','-Dminecraft.launcher.','-XX:HeapDumpPath=','-XX:StackShadowPages=')):continue
  if item=='--enable-native-access=ALL-UNNAMED':continue
  preserve.append(item)
 if shadow_for_26_2:
  assert metadata['id']=='26.2'
  required.insert(0,'-XX:StackShadowPages=32')
 final=preserve+required+game
 assert final.count('-cp')==1
 assert '--enable-native-access=ALL-UNNAMED' in final
 if metadata['id']=='26.3':
  assert '-XX:StackShadowPages=32' in final
  i=final.index('--add-exports');assert final[i+1]=='java.base/jdk.internal.misc=ALL-UNNAMED'
 assert '-Xss1M' not in final # x86-only rule must not be applied to these x64 tests.
 path.write_text('\n'.join(json.dumps(x,ensure_ascii=False) for x in final)+'\n','utf8')
 return required
