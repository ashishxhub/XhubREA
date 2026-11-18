import unittest

from xhubrea_engine.evidence_correlation import correlate_evidence


class EvidenceCorrelationTests(unittest.TestCase):
    def setUp(self):
        self.items = [
            {"id": "apk-1", "source": "apk", "category": "permission",
             "subject": "CAMERA", "observation": "Declared in manifest",
             "evidence_key": " CAMERA ", "confidence": "medium"},
            {"id": "src-1", "source": "source", "category": "api",
             "subject": "CameraManager", "observation": "Reference found in source inventory",
             "evidence_key": "camera", "confidence": "low"},
            {"id": "arch-1", "source": "architecture", "category": "module",
             "subject": "camera module", "observation": "Module path listed",
             "evidence_key": "camera", "confidence": "low"},
            {"id": "solo", "source": "apk", "category": "asset",
             "subject": "logo", "observation": "Asset listed",
             "evidence_key": "logo", "confidence": "high"},
        ]

    def test_correlates_shared_key_across_distinct_sources(self):
        report = correlate_evidence(self.items)
        self.assertEqual(report["summary"]["correlation_groups"], 1)
        group = report["correlations"][0]
        self.assertEqual(group["correlation_key"], "camera")
        self.assertEqual(group["sources"], ["apk", "architecture", "source"])
        self.assertEqual(group["confidence"], "not_assessed")
        self.assertIn("solo", report["unmatched_evidence_ids"])

    def test_same_source_does_not_create_cross_source_correlation(self):
        report = correlate_evidence([
            {"id": "a", "source": "apk", "category": "x", "subject": "one",
             "observation": "first", "evidence_key": "same"},
            {"id": "b", "source": "apk", "category": "x", "subject": "two",
             "observation": "second", "evidence_key": "same"},
        ])
        self.assertEqual(report["correlations"], [])
        self.assertEqual(report["unmatched_evidence_ids"], ["a", "b"])

    def test_deterministic_for_input_order(self):
        self.assertEqual(correlate_evidence(self.items),
                         correlate_evidence(list(reversed(self.items))))

    def test_rejects_duplicate_ids(self):
        duplicate = self.items + [dict(self.items[0])]
        with self.assertRaisesRegex(ValueError, "Duplicate evidence id"):
            correlate_evidence(duplicate)

    def test_rejects_missing_or_invalid_fields(self):
        with self.assertRaises(ValueError):
            correlate_evidence([{"id": "x"}])
        bad = dict(self.items[0], confidence="certain")
        with self.assertRaisesRegex(ValueError, "confidence"):
            correlate_evidence([bad])

    def test_enforces_item_limit(self):
        many = [
            {"id": str(i), "source": "s", "category": "c", "subject": "x",
             "observation": "y", "evidence_key": "k"}
            for i in range(101)
        ]
        with self.assertRaisesRegex(ValueError, "at most 100"):
            correlate_evidence(many)

    def test_does_not_claim_causation_or_upgrade_confidence(self):
        report = correlate_evidence(self.items)
        limitations = " ".join(report["limitations"])
        self.assertIn("not proof", limitations)
        self.assertEqual(report["correlations"][0]["confidence"], "not_assessed")


if __name__ == "__main__":
    unittest.main()
