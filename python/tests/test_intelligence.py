"""Tests for deterministic evidence structuring."""
import unittest

from xhubrea_engine.intelligence import structure_findings


class IntelligenceTests(unittest.TestCase):
    def setUp(self):
        self.items = [
            {"evidence_id": "E1", "source": "manifest", "observation": "Internet permission declared.", "confidence": "high"},
            {"evidence_id": "E2", "source": "manifest", "observation": "Camera permission declared.", "confidence": "medium"},
            {"evidence_id": "E3", "source": "dex header", "observation": "One DEX file listed.", "confidence": "unverified"},
        ]

    def test_structures_evidence_and_confidence(self):
        result = structure_findings(self.items)
        self.assertEqual(result["evidence_count"], 3)
        self.assertEqual(result["evidence"][0]["classification"], "operator_supplied_observation")
        self.assertEqual(result["evidence"][0]["confidence"], "high")
        self.assertEqual(result["evidence"][0]["verification_status"], "not_independently_verified")

    def test_groups_shared_source_without_claiming_causation(self):
        result = structure_findings(self.items)
        self.assertEqual(result["source_correlations"][0]["evidence_ids"], ["E1", "E2"])
        self.assertIn("not_proof_of_causation", result["source_correlations"][0]["relationship"])

    def test_rejects_duplicate_ids(self):
        duplicate = [self.items[0], {**self.items[1], "evidence_id": "E1"}]
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            structure_findings(duplicate)

    def test_rejects_unknown_confidence(self):
        invalid = [{**self.items[0], "confidence": "99%"}]
        with self.assertRaisesRegex(ValueError, "confidence"):
            structure_findings(invalid)

    def test_rejects_empty_or_too_many(self):
        with self.assertRaises(ValueError):
            structure_findings([])
        with self.assertRaisesRegex(ValueError, "At most 12"):
            structure_findings(self.items * 5)


if __name__ == "__main__":
    unittest.main()
