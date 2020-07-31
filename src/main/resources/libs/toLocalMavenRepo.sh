mvn install:install-file \
   -Dfile=./DLVwrapper-v4.20.jar \
   -DgroupId=dlv-wrapper \
   -DartifactId=dlv-wrapper \
   -Dversion=4.20 \
   -Dpackaging=jar \
   -DlocalRepositoryPath=./repo \
   -DgeneratePom=true

mvn install:install-file \
   -Dfile=./RandomProgramGenerator-v1.0.jar \
   -DgroupId=mat-unical \
   -DartifactId=random-qbf-generator \
   -Dversion=1.0 \
   -Dpackaging=jar \
   -DlocalRepositoryPath=./repo \
   -DgeneratePom=true
