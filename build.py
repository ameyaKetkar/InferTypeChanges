import subprocess
import os
from shutil import copyfile

os.chdir("Models")
o = subprocess.Popen("mvn clean install", shell=True).wait()
print(o)
os.chdir("..")
os.chdir("RefactoringMiner")
o = subprocess.Popen("./gradlew jar", shell=True).wait()
print(o)
os.chdir("..")
copyfile('RefactoringMiner/build/libs/RefactoringMiner-2.1.0.jar', "lib/RefactoringMiner-2.1.0.jar")
o = subprocess.Popen("mvn install:install-file -Dfile=lib/Models-1.0-SNAPSHOT.jar -DgroupId=com.t2r -DartifactId=Models -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true", shell=True).wait()
o = subprocess.Popen("mvn install:install-file -Dfile=lib/RefactoringMiner-2.1.0.jar -DgroupId=com.rminer -DartifactId=AmeyaRMiner -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true", shell=True).wait()
o = subprocess.Popen("mvn clean install", shell=True).wait()
os.system("pwd")