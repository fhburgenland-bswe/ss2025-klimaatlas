<?php
header('Access-Control-Allow-Origin: *');
header('Content-Type: text/plain');

$file = __DIR__ . '/health-risk-data.txt';

if (file_exists($file)) {
    echo file_get_contents($file);
} else {
    echo 'NOT_LOADED';
}
