# dphe-to-json

**dphe-to-json** is a small Java utility that generates **JSON Schemas** directly from Java classes within the [DeepPhe](https://github.com/DeepPhe) project ecosystem.

---

## 🧩 Overview

This tool clones a specified GitHub repository (such as `dphe-neo4j`), traverses the Java source directory, and converts each eligible class into a JSON Schema file.  
It’s designed to support automated schema generation pipelines for DeepPhe data models and related ontology-driven projects.

---

## ⚙️ How It Works

1. **Clones** a GitHub repository and branch (e.g., `master`).
2. **Scans** a target Java source directory for model classes.
3. **Generates** JSON Schema files using the [VicTools JSON Schema Generator](https://github.com/victools/jsonschema-generator).
4. **Outputs** all schemas to a versioned directory such as `schemas/v0.7.0/`.

---

## 🧱 Example Usage

```bash
java -jar target/dphe-to-json-1.0-SNAPSHOT-shaded.jar \
  https://github.com/DeepPhe/dphe-neo4j.git \
  master \
  src/main/java/org/healthnlp/deepphe/neo4j/node/xn \
  ./schemas/v0.7.0

##🚀 GitHub Actions Automation

The repository includes a workflow (.github/workflows/generate-schemas.yml) that:

Builds the shaded JAR with Maven.

Generates schemas automatically.

Opens a pull request with any updates to the schemas/vX.Y.Z/ directory.
