<?php
$rfid = $_GET['rfid'];
$gps = $_GET['gps'];

$conn = new mysqli("localhost", "username", "password", "database");
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

$sql = "INSERT INTO logs (rfid, gps) VALUES ('$rfid', '$gps')";
if ($conn->query($sql) === TRUE) {
    echo "Data saved";
} else {
    echo "Error: " . $conn->error;
}
$conn->close();
?>

