# setorku-api-automation
# Setorku Application - API Test Automation Framework

Repository ini berisi *framework automation testing* untuk API **Setorku Application**, dibangun menggunakan **Katalon Studio**, **Groovy**, dan **BDD Cucumber**.

---

## 🛠️ Tech Stack & Prerequisites
* **Testing Tool:** Katalon Studio (v8.x atau versi terbaru) / Katalon Runtime Engine (KRE)
* **Bahasa Pemrograman:** Java / Groovy
* **Framework:** BDD Cucumber (Gherkin)
* **Build Tool:** Gradle (bawaan Katalon Studio)

---

## 📁 Struktur Repository

```text
setorku-api-automation/
├── Include/
│   ├── features/
│   │   └── SetorkuApiFlow.feature               # Skenario BDD Gherkin (TS-01 s/d TS-13)
│   └── scripts/
│       └── groovy/
│           └── com/setorku/steps/
│               └── SetorkuApiSteps.groovy       # Step Definition / Glue Code Katalon
├── Keywords/
│   └── com/setorku/utils/
│       └── SignatureUtil.groovy                  # Custom Keyword kalkulasi SHA-1 Signature
├── Profiles/
│   └── default.glbl                              # Environment Variables (BaseURL & SecretKey)
├── .gitignore                                    # Konfigurasi ignore file Katalon
├── QA_Technical_Assessment_Report_Setorku.pdf    # PDF Report & Bug List
└── README.md                                     # Dokumentasi Strategi & Panduan Running
