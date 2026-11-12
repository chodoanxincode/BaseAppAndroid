"""Đọc file uiautomator dump, in các node có chữ / bounds tràn khỏi node cha hoặc khỏi màn hình."""
import re, sys, xml.etree.ElementTree as ET

def box(n):
    m = re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))
    return tuple(map(int, m))

root = ET.parse(sys.argv[1]).getroot()
screen = None
issues = []
def walk(node, parent_box):
    global screen
    for child in node:
        if child.tag != "node":
            continue
        b = box(child)
        if screen is None:
            screen = b
        label = child.get("text") or child.get("content-desc") or ""
        rid = child.get("resource-id", "").split("/")[-1]
        if parent_box and (b[0] < parent_box[0] or b[1] < parent_box[1] or b[2] > parent_box[2] or b[3] > parent_box[3]):
            if child.get("scrollable") != "true" and b[2] > b[0]:
                issues.append(f"tràn cha   {rid:28} {label[:40]!r:44} {b} cha={parent_box}")
        if screen and (b[0] < screen[0] or b[1] < screen[1] or b[2] > screen[2] or b[3] > screen[3]):
            issues.append(f"tràn màn   {rid:28} {label[:40]!r:44} {b}")
        if label and (b[2] - b[0] <= 0 or b[3] - b[1] <= 0):
            issues.append(f"rỗng       {rid:28} {label[:40]!r:44} {b}")
        walk(child, b)
walk(root, None)
print(f"Màn hình: {screen}")
print("\n".join(issues) if issues else "OK: không thấy view tràn")
