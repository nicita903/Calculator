"""Device smoke checks. Requires adb on PATH; changes only Calculator's own UI/settings."""
import subprocess, time, re, shutil, xml.etree.ElementTree as ET
from pathlib import Path
ADB = shutil.which("adb") or r"E:\programs\Android_SDK_Location\platform-tools\adb.exe"
ROOT = Path(__file__).resolve().parent

def adb(*args):
    return subprocess.check_output([ADB, *args], encoding="utf-8")
def snap():
    time.sleep(.3)
    adb("shell", "uiautomator", "dump", "/sdcard/calculator-refinement.xml")
    adb("pull", "/sdcard/calculator-refinement.xml", str(ROOT / "device-ui.xml"))
    return ET.parse(ROOT / "device-ui.xml").getroot()
def text_nodes(root, label):
    return [n for n in root.iter("node") if n.get("text") == label or n.get("content-desc") == label]
def center(node):
    a,b,c,d = map(int,re.findall(r"\d+",node.get("bounds")))
    return (a+c)//2, (b+d)//2
def tap(x,y): adb("shell","input","tap",str(x),str(y))
def choose(root,label): tap(*center(text_nodes(root,label)[0]))
def expression(root):
    bounds = next(n.get("bounds") for n in root.iter("node") if n.get("content-desc")=="Mathematical expression")
    return next((n.get("text") for n in root.iter("node") if n.get("bounds")==bounds and n.get("text")), "")
def menu(root): choose(root,"Open navigation drawer"); return snap()

r=snap()
keys={k:center(text_nodes(r,k)[-1]) for k in ["AC","1","2","3","4","7","8","9","+","=","\u232b"]}
def key(k): tap(*keys[k])
key("AC")
for k in "1234":key(k)
r=snap(); assert expression(r)=="1234",expression(r)
# Editor line is right aligned. The center of its final four glyphs is observable in the screenshot.
editor=text_nodes(r,"Mathematical expression")[0]
a,b,c,d=map(int,re.findall(r"\d+",editor.get("bounds")))
# Tap in the first quarter of the text (cursor after the first digit).
tap(c-190,(b+d)//2);key("9");r=snap()
assert expression(r)=="19234",expression(r)
key("\u232b");r=snap();assert expression(r)=="1234"
# Long press selects all and the next key replaces it.
adb("shell","input","swipe",str(c-120),str((b+d)//2),str(c-120),str((b+d)//2),"700")
key("7");r=snap();assert expression(r)=="7"
key("AC")
for k in "1234123412341234":key(k)
r=snap();assert expression(r)=="1234123412341234"
ime=adb("shell","dumpsys","input_method")
assert not re.search(r"(?:mInputShown|isInputViewShown)=true",ime)
assert not any(n.get("class")=="android.widget.EditText" for n in r.iter("node"))
assert not any(n.get("class")=="android.widget.ScrollView" for n in r.iter("node"))
r=menu(r);choose(r,"Settings");r=snap();choose(r,"Maximum");r=snap()
assert text_nodes(r,"Intensity · Maximum")
adb("shell","am","force-stop","com.example.calculator")
adb("shell","am","start","-n","com.example.calculator/.MainActivity")
r=snap();r=menu(r);choose(r,"Settings");r=snap();assert text_nodes(r,"Intensity · Maximum")
# Leave the requested default intensity selected after persistence testing.
choose(r,"Strong");r=snap();assert text_nodes(r,"Intensity · Strong")
adb("shell","input","keyevent","4");r=snap();assert text_nodes(r,"Calculator")
key("AC");key("7");key("+");key("8");key("=");r=snap();assert expression(r)=="7+8";assert text_nodes(r,"15")
r=menu(r);adb("shell","input","keyevent","4");r=snap();assert expression(r)=="7+8"
adb("shell","screencap","-p","/sdcard/calculator-refined.png")
adb("pull","/sdcard/calculator-refined.png",str(ROOT/"calculator-refined.png"))
print("PASS: cursor insertion/deletion, select-all replacement, rapid input, no IME, fixed layout, saved haptic intensity, drawer/Back, arithmetic")
