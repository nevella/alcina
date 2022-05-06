# Patch to support detail logging of batch update exceptions

Adds a static exceptionConsumer which can be set by upstream code for notification of 'which entity caused the problem'

build:
cd <hibernate-orm git dir>/hibernate-core
git checkout 5.3.20

# jdk8, gradle 4
export JAVA_HOME=/Library/Java/alt.jvm/jdk1.8.0_152.jdk/Contents/Home
wget https://downloads.gradle-dn.com/distributions/gradle-4.10.2-bin.zip
 echo 'export PATH="/my_gradle_install/bin:$PATH"' >> ~/.profile
 source ~/.profile

gradle build -x test



diff --git a/hibernate-core/src/main/java/org/hibernate/jdbc/Expectations.java b/hibernate-core/src/main/java/org/hibernate/jdbc/Expectations.java
index e540b4b..9eac96a 100644
--- a/hibernate-core/src/main/java/org/hibernate/jdbc/Expectations.java
+++ b/hibernate-core/src/main/java/org/hibernate/jdbc/Expectations.java
@@ -10,6 +10,7 @@
 import java.sql.PreparedStatement;
 import java.sql.SQLException;
 import java.sql.Types;
+import java.util.function.BiConsumer;
 
 import org.hibernate.HibernateException;
 import org.hibernate.StaleStateException;
@@ -46,12 +47,19 @@
 		}
 
 		public final void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition) {
-			rowCount = determineRowCount( rowCount, statement );
-			if ( batchPosition < 0 ) {
-				checkNonBatched( rowCount );
-			}
-			else {
-				checkBatched( rowCount, batchPosition );
+			try{
+				rowCount = determineRowCount( rowCount, statement );
+				if ( batchPosition < 0 ) {
+					checkNonBatched( rowCount );
+				}
+				else {
+					checkBatched( rowCount, batchPosition );
+				}
+			}catch(RuntimeException e){
+				if(exceptionConsumer!=null){
+					exceptionConsumer.accept(e, statement);
+				}
+				throw e;
 			}
 		}
 
@@ -183,4 +191,6 @@
 
 	private Expectations() {
 	}
+	
+	public static BiConsumer<RuntimeException,PreparedStatement> exceptionConsumer;
 }

 
 