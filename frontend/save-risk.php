<?php
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type');
header('Access-Control-Allow-Methods: POST, OPTIONS');

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $content = $_POST['text'] ?? '';

    $file = __DIR__ . '/health-risk-data.txt';

    file_put_contents($file, $content);

    echo 'saved successfully';
}
