package com.vt.smart.switchapp.connectivity

// Foreground service removed on purpose.
// File transfer now runs in TransferManager (process level worker thread with
// WakeLock + WifiLock), so no foreground service / notification is required.
// This file is intentionally empty and can be deleted from the project.
