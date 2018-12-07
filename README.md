# merge-coupling

# About
This project is an adaptation of the SemanticSimilarityJava tool that was developed by Dr. Nemitari Ajienka and it aims to identify the conceptual coupling across branches of Git projects.

# Team
Cristiane da Silva Rodrigues Pereira (UFF, Brazil)

Leonardo Gresta Paulino Murta (UFF, Brazil)

# Usage
1. Create the gitProjects and projects folders in your home directory;
2. Clone the Git project and save it to the gitProjects folder;
3. In your IDE (e.g. NetBeans), open the ConceptualCoupling.java class and execute;
4. This will identify the merges, build the corpus for each of them (eliminate common key words, split and stem class and method identifiers) and compute their semantic similarity using the Vector Space Model (VSM) technique;
5. The results will be stored in files with the .txt extension to the projects\outputs folder;
6. The FinalResultMergeConceptualCoupling.txt file has the number of couplings and the average of the similarity values of each merge.
