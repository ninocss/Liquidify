"""Apply only reviewed, exact ABI mappings. Never guess a class or member name."""
import argparse
import json
import re
from pathlib import Path


def remap(text: str, mapping: dict) -> str:
    for annotation in mapping.get("stripAnnotations", []):
        text = re.sub(
            r"(?m)^\s*\.annotation (?:runtime|build) " + re.escape(annotation) + r"\n.*?^\s*\.end annotation\s*\n",
            "\n", text, flags=re.DOTALL,
        )
    # Longest first avoids remapping a nested owner before its full descriptor.
    members = mapping.get("members", {})
    types = mapping.get("types", {})
    if members:
        pattern = re.compile("|".join(re.escape(s) for s in sorted(members, key=len, reverse=True)))
        text = pattern.sub(lambda m: members[m.group()], text)
    if types:
        pattern = re.compile("|".join(re.escape(s) for s in sorted(types, key=len, reverse=True)))
        text = pattern.sub(lambda m: types[m.group()], text)
    return text


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--mapping", type=Path, required=True)
    p.add_argument("--input", type=Path, required=True)
    p.add_argument("--output", type=Path, required=True)
    args = p.parse_args()
    if args.input.resolve() == args.output.resolve():
        p.error("Input and output directories must differ")
    mapping = json.loads(args.mapping.read_text())
    count = 0
    for src in sorted(args.input.rglob("*.smali")):
        dest = args.output / src.relative_to(args.input)
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(remap(src.read_text(encoding="utf-8"), mapping), encoding="utf-8", newline="\n")
        count += 1
    print(f"Remapped {count} extension classes")


if __name__ == "__main__":
    main()
