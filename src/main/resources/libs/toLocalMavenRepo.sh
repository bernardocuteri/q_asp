mvn install:install-file \
   -Dfile=./DLVwrapper-v4.2.jar \
   -DgroupId=dlv-wrapper \
   -DartifactId=dlv-wrapper \
   -Dversion=4.2 \
   -Dpackaging=jar \
   -DlocalRepositoryPath=. \
   -DgeneratePom=true

mvn install:install-file \
   -Dfile=./RandomProgramGenerator-v1.0.jar \
   -DgroupId=mat-unical \
   -DartifactId=random-qbf-generator \
   -Dversion=1.0 \
   -Dpackaging=jar \
   -DlocalRepositoryPath=. \
   -DgeneratePom=true
