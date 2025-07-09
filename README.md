# 📱 Modern Todo App

A feature-rich, native Android todo application built with **Kotlin** and **Room Database**, following clean architecture principles and modern Android development practices.

## 🎯 Features

### 📋 **Core Functionality**
- ✅ **Create, Edit, Delete todos** with comprehensive form validation
- 🎯 **Priority levels** (High/Medium/Low) with color-coded visual indicators
- 📅 **Due date & time management** with date/time pickers
- 🔔 **Smart reminders** with notification scheduling using AlarmManager
- ✏️ **Rich text descriptions** with multi-line support

### 🔍 **Advanced Search & Filtering**
- 🔎 **Real-time search** across todo titles with instant results
- 📊 **Multiple sorting options**:
  - Newest First (default)
  - Priority-based sorting
  - Due date sorting (earliest/latest)
- 🎛️ **Intuitive toolbar controls** with search bar integration

### ⚠️ **Smart Overdue Detection**
- 🚨 **Visual indicators** for overdue tasks with warning icons
- 📊 **Dynamic date formatting** showing days overdue
- 🎨 **Color-coded urgency** (red text for overdue items)

### ✅ **Completed Todos Management**
- 📜 **Dedicated completed todos view** with distinct styling
- 🎬 **Smooth slide animations** when marking todos as incomplete
- 🎨 **Visual differentiation** (strikethrough text, muted colors, transparency)
- ↩️ **One-tap reactivation** to move todos back to active list

### ⚙️ **Settings & Data Management**
- 📊 **App statistics** (total, active, completed todos, completion rate)
- 🗑️ **Bulk operations**:
  - Clear all todos
  - Clear completed todos only
  - Clear all scheduled reminders
- 📋 **Comprehensive app information**

## 🏗️ Technical Architecture

### **MVVM Architecture Pattern**
```
UI Layer (Activities/Fragments)
    ↓
ViewModel Layer (State Management)
    ↓
Domain Layer (Business Logic)
    ↓
Repository Layer (Data Abstraction)
    ↓
Data Layer (Room Database)
```

### **Technology Stack**
- **Language**: Kotlin 100%
- **Database**: Room Persistence Library
- **Architecture**: MVVM with LiveData/StateFlow
- **UI**: Material Design 3 components
- **Async**: Coroutines + Flow for reactive programming
- **DI**: Manual dependency injection (ViewModelFactory pattern)
- **Notifications**: AlarmManager + NotificationManager

### **Key Technical Decisions**

#### **Reactive Data Flow**
```kotlin
Room Database → Flow<List<Todo>> → ViewModel → StateFlow → UI Updates
```
- Real-time UI updates using Kotlin Flows
- Automatic data synchronization across screens
- No manual refresh needed

#### **State Management**
```kotlin
data class TodoViewUiState(
    val isLoading: Boolean = false,
    val todos: List<Todo> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val currentSortOption: SortOption = SortOption.CREATED_DESC
)
```

#### **Database Schema**
```kotlin
@Entity(tableName = "todos")
data class Todo(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    val title: String,
    val description: String,
    val priority: Priority,
    val dueDate: LocalDateTime,
    val reminderDateTime: LocalDateTime? = null,
    val isCompleted: Boolean = false
)
```

## 📸 Screenshots

| Main Screen | Completed Todos | Add Todo | Sort Options |
|-------------|----------------|----------|--------------|
| ![Main](screenshot1.png) | ![Completed](screenshot2.png) | ![Add](screenshot3.png) | ![Sort](screenshot4.png) |

*The app features a clean, Material Design interface with intuitive navigation and visual feedback*

## 🎨 UI/UX Highlights

### **Material Design Implementation**
- 🎨 **Consistent color scheme** with priority-based card backgrounds
- 📱 **Responsive layouts** using ConstraintLayout and CardView
- ✨ **Smooth animations** for state transitions and user interactions
- 🔄 **Loading states** with progress indicators

### **User Experience Features**
- 🎯 **One-handed usage** with bottom-positioned FAB
- ⚡ **Instant feedback** with toast messages and visual confirmations
- 🔒 **Confirmation dialogs** for destructive actions
- 📱 **Proper keyboard handling** in search mode

## 🔧 Advanced Features

### **Smart Reminder System**
- ⏰ **Precise timing** using AlarmManager for exact notifications
- 🔔 **Permission handling** for Android 13+ notification permissions
- 🎯 **Automatic cancellation** when todos are completed or deleted

### **Search Implementation**
- 🔍 **Database-level filtering** using Room SQL queries
- ⚡ **Debounced input** to prevent excessive database calls
- 🎛️ **Clean state management** with search activation/deactivation

### **Data Persistence**
- 💾 **Robust error handling** with Result wrapper pattern
- 🔄 **Automatic data validation** at multiple layers
- 📊 **Efficient queries** with proper indexing

## 🚀 Getting Started

### **Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.8+

### **Installation**
```bash
# Clone the repository
git clone https://github.com/yourusername/todo-app.git

# Open in Android Studio
cd todo-app
# File -> Open -> Select project folder

# Build and run
./gradlew assembleDebug
```

### **Key Dependencies**
```kotlin
// Room Database
implementation "androidx.room:room-runtime:2.5.0"
implementation "androidx.room:room-ktx:2.5.0"

// Coroutines & Flow
implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

// Lifecycle & ViewModel
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.7.0"

// Material Design
implementation "com.google.android.material:material:1.9.0"
```

## 🔮 Future Enhancements

- 📊 **Data Export/Import** (CSV, JSON)
- 🌙 **Dark Mode** support
- 📱 **Widget** for home screen
- 🔄 **Cloud Sync** with Firebase
- 📈 **Analytics** and productivity insights
- 🎨 **Custom themes** and color schemes

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Developer

Saptarshi Das
- 📧 Email: saptarshidas0101@gmail.com
- 💼 LinkedIn: [Saptarshi Das](https://www.linkedin.com/in/saptarshi-das-204797270/)
- 🐱 GitHub: [@babatezpur](https://github.com/babatezpur)

---

*Built with ❤️ using modern Android development practices*
