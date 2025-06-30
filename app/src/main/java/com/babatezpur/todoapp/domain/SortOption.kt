package com.babatezpur.todoapp.domain

// First, let's define our sort options
enum class SortOption {
    CREATED_DESC,    // Default - newest first
    CREATED_ASC,     // Oldest first
    PRIORITY,        // P1 → P2 → P3
    DUE_DATE_ASC,    // Earliest due first
    DUE_DATE_DESC    // Latest due first
}