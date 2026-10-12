import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from check_linkage import audit


def cls(superclass="Ljava/lang/Object;", methods=(), fields=(), interfaces=()):
    return dict(super=superclass, methods=list(methods), fields=list(fields), interfaces=list(interfaces))


class LinkageTests(unittest.TestCase):
    def test_missing_method_is_not_satisfied_by_object(self):
        report = audit({"LHost;": cls()}, {}, dict(types=[], methods=["LHost;->missing()V"], fields=[]))
        self.assertFalse(report["passed"])
        self.assertEqual(report["missingMethods"], ["LHost;->missing()V"])

    def test_inherited_members_and_object_members_resolve(self):
        host = {"LBase;": cls(methods=["run()V"], fields=["x:I"]), "LChild;": cls("LBase;")}
        refs = dict(types=[], methods=["LChild;->run()V", "LChild;->toString()Ljava/lang/String;"], fields=["LChild;->x:I"])
        self.assertTrue(audit(host, {}, refs)["passed"])

    def test_constructor_is_not_inherited(self):
        host = {"LBase;": cls(methods=["<init>(I)V"]), "LChild;": cls("LBase;")}
        self.assertFalse(audit(host, {}, dict(types=[], methods=["LChild;-><init>(I)V"], fields=[]))["passed"])

    def test_duplicate_host_classes_fail(self):
        self.assertFalse(audit({"LHost;": cls()}, {"LHost;": cls()}, dict(types=[], methods=[], fields=[]))["passed"])

    def test_missing_array_element_type_fails(self):
        report = audit({}, {}, dict(types=["[[LMissing;", "[I"], methods=[], fields=[]))
        self.assertEqual(report["missingTypes"], ["LMissing;"])
        self.assertFalse(report["passed"])

    def test_interface_diamond_does_not_mask_later_parent(self):
        host = {"LRoot;": cls(), "LA;": cls(interfaces=["LRoot;"]),
                "LB;": cls(methods=["run()V"], interfaces=["LRoot;"]),
                "LChild;": cls(interfaces=["LA;", "LB;"])}
        self.assertTrue(audit(host, {}, dict(types=[], methods=["LChild;->run()V"], fields=[]))["passed"])


if __name__ == "__main__":
    unittest.main()
