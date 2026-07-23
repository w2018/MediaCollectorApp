<?php
error_reporting(E_ALL);
ini_set('display_errors', '1');
header('Content-Type: text/plain; charset=utf-8');
echo "PHP Version: " . PHP_VERSION . "\n";
echo "SQLite: " . (extension_loaded('sqlite3') ? 'OK' : 'NO') . "\n";
echo "PDO_SQLite: " . (extension_loaded('pdo_sqlite') ? 'OK' : 'NO') . "\n";
echo "REQUEST_URI: " . ($_SERVER['REQUEST_URI'] ?? 'N/A') . "\n";
echo "SCRIPT_NAME: " . ($_SERVER['SCRIPT_NAME'] ?? 'N/A') . "\n";
echo "SCRIPT_FILENAME: " . ($_SERVER['SCRIPT_FILENAME'] ?? 'N/A') . "\n";
echo "PATH_INFO: " . ($_SERVER['PATH_INFO'] ?? 'N/A') . "\n";
$dbPath = __DIR__ . '/data/media_collector.db';
echo "DB_PATH: " . $dbPath . "\n";
echo "DATA_DIR_EXISTS: " . (is_dir(dirname($dbPath)) ? 'OK' : 'NO') . "\n";
echo "DATA_DIR_WRITABLE: " . (is_writable(dirname($dbPath)) ? 'OK' : 'NO') . "\n";
try {
    $pdo = new PDO('sqlite:' . $dbPath);
    echo "PDO: OK\n";
} catch (Exception $e) {
    echo "PDO: FAIL - " . $e->getMessage() . "\n";
}
