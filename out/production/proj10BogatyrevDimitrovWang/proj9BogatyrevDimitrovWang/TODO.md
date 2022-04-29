- Your task will be to fill in the bodies of all of the methods in Parser.java. 

- For this proj9BogatyrevDimitrovWang, the parser can ignore and throw away any comment tokens sent to it by the Scanner.
- Other than adding "proj8your-last-names." to the front of the package in the package statement in each file, 
do not modify any of the files that I am giving you except the Parser.java file.
   - You are, of course, allowed to add additional methods to Parser.java and add additional files and classes, if appropriate, to make your design more elegant.

- If there are no errors during the parse, the parse() method should return the AST it created. 
  - If the parse() method finds an error, it should report it to the ErrorHandler and then throw a CompilationException.

- Finally, add a main method to your Parser.java file for testing purposes.
- It should take any number of command line arguments. 
Those arguments should be names of files. The main method should loop through the files, scanning and parsing each one, 
and printing to System.out the result, namely the name of the file and either the error messages when the file was 
scanned and parsed or a message that scanning and parsing were successful for that file.

- As part of your testing, I would like you to create and test a file named "ParserTest<your-last-names>.btm" that contains a legal Bantam source program 
that tests all of the Bantam grammar rules listed on page 49 in the Bantam Java Manual. 
Turn in that file with your proj9BogatyrevDimitrovWang. 
(Note: "Member -> Method | Field" is actually 2 rules: "Member -> Method" and "Member -> Field", so be sure to test all such rules.)

- The treedrawer package that I am providing for you draws an AST in a window (using the Java Swing framework instead of the JavaFX framework). 
To draw an AST, you need only create a Drawer object and call its draw() method. 
I want you to call this method in your Parser's main method if the parsing was successful so that you (and I) can check that the AST was properly constructed.