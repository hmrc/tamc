Marriage Allowance Application
====================================================================

Requirements
------------

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.


How to contribute
-----------

If you want to contribute any changes to Marriage Allowance Frontend application, then
 * Go to the [tamc](https://github.com/hmrc/tamc) repository on github.
 * Click the “Fork” button at the top right.
 * You’ll now have your own copy of that repository in your github account.
 * Open a terminal/shell and clone directory using below command

  ```$ git clone git@github.com:username/tamc.git```

  where 'username' is your github user name

* You’ll now have a local copy of your version of that repository.
* Change to project directory tamc-frontend and start with changes.

Post code changes check
-----------

Once you are done with the changes make sure that:
* all test cases successful. Use below command to run the test cases
 
  ```$ sbt test```

* all your changes are covered by unit test cases. If not, please write more test cases.
* code coverage does not go below already existing code coverage. Use below command to run coverage report
 
  ```$ sbt clean converage test```

* you have taken latest code from master before you raise 
* there are no merge conflicts in your pull request.
* you have provided relavant comments while comitting changes and while raising pull request. 
 
What happens next
------------

Once you have raised pull request for the changes, tamc-frontend owner team will recieve an email. The team will review these changes and will advise you further. They will:
* check for unit test code coverage for the changes.
* check the overall test coverage for the whole project.
* review the changes and may ask you for further enhancements.
* merge your changes and you will recieve a mail for the same.

