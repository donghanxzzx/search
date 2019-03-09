cd C:\Users\dongh\dev\code\private_code\search
mvn clean install -DskipTests
scp .\target\search-0.0.1-SNAPSHOT.jar rrot:/data/app/search