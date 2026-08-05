from pathlib import Path
import re

script_path = Path(__file__).with_name("unify_daily_systems_patch.py")
source = script_path.read_text(encoding="utf-8")
needle = '    2,\n    "clear daily state",\n)'
replacement = '    1,\n    "clear daily state",\n)'
if source.count(needle) != 1:
    raise RuntimeError("clear daily state expectation marker not found")
source = source.replace(needle, replacement, 1)
namespace = {"__file__": str(script_path), "__name__": "__main__"}
exec(compile(source, str(script_path), "exec"), namespace)

# The load-failure and missing-section branches use different indentation.
# Add the reset to any remaining branch that still clears Sasurai then encyclopedia.
vm_path = Path(__file__).resolve().parents[2] / "app/src/main/java/com/atuy/yws1editor/MainViewModel.kt"
vm = vm_path.read_text(encoding="utf-8")
pattern = re.compile(
    r"^(?P<indent>\s*)sasuraiResidents = emptyList\(\),\n"
    r"(?P=indent)encyclopediaEntries = emptyList\(\),",
    re.MULTILINE,
)
vm, count = pattern.subn(
    lambda match: (
        f"{match.group('indent')}sasuraiResidents = emptyList(),\n"
        f"{match.group('indent')}dailySystems = DailySystemState.EMPTY,\n"
        f"{match.group('indent')}encyclopediaEntries = emptyList(),"
    ),
    vm,
)
if count not in (0, 1):
    raise RuntimeError(f"unexpected remaining daily reset branches: {count}")
vm_path.write_text(vm, encoding="utf-8")
print(f"Patched {count} additional reset branch(es).")
