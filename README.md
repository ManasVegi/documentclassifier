# documentclassifier
Learning NLP by trying to cluster documents based on their tfidf representations and then measuring the clustering performance based on pre-assigned folders. Experimenting with Kmeans and Kmeans++ for clustering.

## Data
The data.txt file holds the directories for the documents. The documents are copyrighted and hidden. The structure can be understood as, data/C1 is the label for all the files in that folder, data/C4 is the label for all the... and so on.

## Setup
Project was done in IntellijIDEA. Install stanford-core-nlp's 4.5.1 jar file and add that as a library in the Intellij project structure. 

*Note:* Make sure there are no other core-nlp jars in the libraries from a maven source because those seem to cause compile errors due to missing dependencies.

## Testing
Can test different hyperparameters to the algorithm in the Main.java file where you can set the seed for the initial centroids of KMeans algorithm, or decide which similarity measure to use when clustering.
