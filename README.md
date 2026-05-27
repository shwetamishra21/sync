# Sarcasm-Detection-NLP
**CSE3246 — Natural Language Processing | Topic 12: Sarcasm Detection with Sentiment Analysis**

## Team
| Member | Role | Notebooks |
|--------|------|-----------|
| Member 1 (you) | ML Pipeline, TF-IDF, BERT, Metrics, Demo | `02_tfidf_sklearn.ipynb`, `03_bert_finetune.ipynb`, `04_demo.ipynb` |
| Member 2 | Rule-based spaCy, Report, Slides 1–8 | `01_rule_based.ipynb` |

## Dataset
`raquiba/Sarcasm_News_Headline` — ~28,000 news headlines (TheOnion + HuffPost), binary classification.
Load via: `from datasets import load_dataset; ds = load_dataset('raquiba/Sarcasm_News_Headline')`

## Methods
| # | Method | Owner | Key Library |
|---|--------|-------|-------------|
| 1 | Rule-based (incongruity, hyperbole, punctuation) | Member 2 | spaCy |
| 2 | TF-IDF + Logistic Regression / SVM | Member 1 | scikit-learn |
| 3 | Fine-tuned BERT (bert-base-uncased) | Member 1 | HuggingFace Transformers |

## Repo Structure
```
Sarcasm-Detection-NLP/
├── notebooks/
│   ├── 01_rule_based.ipynb        ← Member 2
│   ├── 02_tfidf_sklearn.ipynb     ← Member 1
│   ├── 03_bert_finetune.ipynb     ← Member 1
│   └── 04_demo.ipynb              ← Member 1
├── outputs/
│   ├── method1/                   ← Member 2 writes
│   ├── method2/                   ← Member 1 writes
│   ├── method3/                   ← Member 1 writes
│   └── final_comparison/          ← Member 1 writes (Day 2 eve)
├── report/                        ← Member 2 owns
├── slides/                        ← Both (Day 3)
├── requirements.txt
└── README.md
```

## Setup
```bash
git clone https://github.com/YOUR_USERNAME/Sarcasm-Detection-NLP
cd Sarcasm-Detection-NLP
pip install -r requirements.txt
```
## Branch Strategy
- `main` — protected, merge via PR only
- `feature/member1-tfidf` — M1 TF-IDF work
- `feature/member1-bert` — M1 BERT work
- `feature/member1-demo` — M1 demo
- `feature/member2-rules` — M2 spaCy work
- `feature/member2-report` — M2 report

## Results (filled after Day 2)
| Method | Accuracy | F1 | MCC | AUC |
|--------|----------|----|-----|-----|
| Rule-based (M1) | TBD | TBD | TBD | — |
| TF-IDF + LR (M2) | TBD | TBD | TBD | TBD |
| BERT fine-tuned (M3) | TBD | TBD | TBD | TBD |

## Evaluation Metrics
All 10 rubric metrics computed: Accuracy, Precision, Recall, F1, Sensitivity, Specificity, FPR, FNR, NPV, FDR, MCC.
