#!/usr/bin/env python3
"""Optional end-to-end checks. First compile with bash check.sh. No dependencies."""
import subprocess,tempfile,pathlib,json
java='java'
cp=str(pathlib.Path(__file__).resolve().parents[1] / 'out/main')
checks=[]
def run(cwd,inputs,*args):
 p=subprocess.run([java,'-cp',cp,'org.qatracker.Main',*args],input=inputs,text=True,capture_output=True,cwd=cwd,timeout=15)
 assert p.returncode==0 and not p.stderr,(p.returncode,p.stderr,p.stdout)
 return p.stdout
def check(cond,label):
 assert cond,label
 checks.append(label)
with tempfile.TemporaryDirectory() as td:
 out=run(td,'2\n5\nabc\n0\n1\n   \n1\nTitle\nDesc\nPre\n \n1\nLogin works\nValid credentials\nAccount exists\nDashboard\nBAD\nmanual\nAlex\n1\nSearch API\nSearch login\n\n200\nAUTOMATED\nscripts/test.java\n3\n999\n3\n1\ntext\n9\n1\n3\n2\n2\n2\n4\nLoGiN\n4\n\n4\nno-match-xyz\n5\n6\n')
 check('No test cases yet.' in out,'empty-list message')
 check('0.0%' in out,'empty report is 0.0%')
 check('whole number' in out and 'option from 1 to 6' in out,'invalid menu input handled')
 check('Title cannot be empty.' in out and 'Expected result cannot be empty.' in out,'blank required fields rejected')
 check('Invalid type.' in out,'invalid type reprompted')
 check('No test case exists with ID 999.' in out,'missing update ID handled')
 check('Invalid status.' in out,'invalid status reprompted')
 check('Updated test case #1 to PASS.' in out and 'Updated test case #2 to FAIL.' in out,'status updates')
 check('No matching test cases.' in out,'no-match search message')
 check('50.0%' in out and 'Saved 2 test case(s).' in out,'report and save')
 check(pathlib.Path(td,'testcases.xml').exists(),'default file created in project directory')
 restarted=run(td,'2\n1\nNext\n\n\nExpected\nManual\n\n5\n6\n')
 check('Loaded 2 test case(s).' in restarted,'startup reload')
 check('Alex' in restarted and 'scripts/test.java' in restarted and 'PASS' in restarted and 'FAIL' in restarted,'type-specific fields and statuses restored')
 check('Created test case #3' in restarted,'IDs continue across application sessions')
 check('33.3%' in restarted,'one-decimal rounding with NOT_RUN included')
 eof=run(td,'1\nunfinished title\n')
 check('Input closed.' in eof and 'Saved 3 test case(s).' in eof,'EOF discards incomplete addition and saves completed records')
 data=pathlib.Path(td,'testcases.xml');data.write_text('BROKEN')
 broken=run(td,'6\n')
 check('Startup stopped to protect existing data.' in broken and data.read_text()=='BROKEN','malformed startup file protected')
with tempfile.TemporaryDirectory() as td:
 proc=subprocess.Popen([java,'-cp',cp,'org.qatracker.Main'],stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,cwd=td)
 prefix=''
 while not prefix.endswith('Enter your choice: '):
  ch=proc.stdout.read(1)
  assert ch, prefix
  prefix+=ch
 pathlib.Path(td,'testcases.xml').mkdir()
 rest,err=proc.communicate('6\n',timeout=15)
 failed=prefix+rest
 check(not err and 'Could not save:' in failed and 'unsaved changes' in failed and 'Goodbye!' not in failed,'save failure reported without false success')
print(json.dumps({'checks':len(checks),'results':checks},indent=2))

