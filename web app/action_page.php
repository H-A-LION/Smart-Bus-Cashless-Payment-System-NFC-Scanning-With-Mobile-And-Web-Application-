<?php
// define variables and set to empty values
$name=_POST["uname"];
$password=_POST["pswd"];
$nameErr = $passwdErr = "";

//check if any variable is empty
if ($_SERVER["REQUEST_METHOD"] == "POST") {
  if (empty($name)) {
    $nameErr = "Name is required";
  } else {
    $name = test_input($name);
  }
  if (empty($_password)) {
    $emailErr = "Password is required";
  } else {
    $password = test_input($password);
  }
 
}

?>

