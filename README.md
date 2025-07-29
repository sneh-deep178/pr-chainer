# PR Chainer 🔗

**Intelligent Git workflow automation for better code reviews**

PR Chainer automatically detects when your repository changes exceed a threshold and helps you create manageable pull request chains for easier code review.

## 🎯 **Features**

### **Smart Branch Comparison**
- **Hierarchical branching**: Compares with parent branch instead of last commit
- **400-line threshold**: Perfect for code review best practices
- **Master protection**: Keeps main branch clean with automated workflows

### **Intelligent Workflows**
```bash
# Example workflow:
master → feature-auth-1 → feature-auth-2 → feature-auth-3
   ↑           ↑              ↑              ↑
  clean    400 lines      400 lines      400 lines
```

### **Automated Git Operations**
- ✅ **Auto-stage** selected files
- ✅ **Auto-commit** with custom messages
- ✅ **Auto-push** current branch
- ✅ **Auto-create** next branch in chain
- ✅ **Auto-switch** to new branch

### **User Experience**
- 🎨 **Professional icons** (light & dark theme support)
- 🚫 **No accidental triggers** (10-second cooldown)
- 📁 **File selection dialog** for staged commits
- 🔄 **Progress indicators** during Git operations

## 🚀 **Installation**

### **From IDE Plugin Repository**
1. Open your JetBrains IDE (IntelliJ IDEA, GoLand, PhpStorm, etc.)
2. Go to `File` → `Settings` → `Plugins`
3. Search for "PR Chainer"
4. Click `Install` and restart IDE

### **Manual Installation**
1. Download the latest `.jar` file from releases
2. Go to `File` → `Settings` → `Plugins` → `⚙️` → `Install Plugin from Disk`
3. Select the downloaded `.jar` file
4. Restart IDE

## 📖 **How It Works**

### **Branch Detection Logic**
```kotlin
// Pattern matching for parent branch detection:
sneh-master-1    → master
sneh-master-2    → sneh-master-1  
sneh-master-3    → sneh-master-2
feature-auth-1   → master
feature-auth-2   → feature-auth-1
```

### **Change Detection**
- **Branch changes**: `git diff parent-branch...HEAD`
- **Working directory**: `git diff HEAD`
- **Total count**: Combined for complete picture

### **Threshold Management**
- **Default**: 400 lines
- **Increase**: +100 lines when threshold exceeded
- **Reset**: Back to 400 after successful workflow

## 🎮 **Usage**

### **Automatic Trigger**
The plugin monitors your changes in real-time. When you exceed 400 lines:

```
📊 Branch 'feature-auth-2' vs 'feature-auth-1': 420/400 lines (105%)

Repository changes exceeded threshold!

Commit current changes and create a new branch?

[Commit & Create Branch]  [Increase Threshold (+100)]  [Cancel & Increase Threshold]
```

### **Master Branch Protection**
When working on `master`:

```
⚠️ You're on the 'master' branch!

📊 Working directory: 450/400 lines (112%)

This will:
1. Prompt you to enter a branch name
2. Create a branch with your-name-1
3. Move your changes to that branch
4. Create another branch with incremented number
5. Keep master clean and protected
```

### **File Selection Dialog**
Choose exactly what to commit:

```
┌─ Select Files and Commit Message ─────────────────┐
│ Commit Message:                                   │
│ ┌─────────────────────────────────────────────┐   │
│ │ feat: implement user authentication system │   │
│ └─────────────────────────────────────────────┘   │
│                                                   │
│ Select files to commit:                           │
│ ☑ src/auth/AuthService.kt                        │
│ ☑ src/auth/UserController.kt                     │
│ ☐ src/config/SecurityConfig.kt                   │
│ ☑ tests/AuthServiceTest.kt                       │
│                                                   │
│ 🔄 Processing... Please wait...                   │
│                                                   │
│         [Commit & Create Branch]  [Cancel]        │
└───────────────────────────────────────────────────┘
```

## 🌟 **Benefits**

### **For Developers**
- **No more huge PRs** → Automatic size management
- **Better code organization** → Logical commit chunks
- **Reduced merge conflicts** → Smaller, focused changes
- **Faster development** → Automated Git workflows

### **For Code Review**
- **Easier to review** → 400-line chunks are manageable
- **Better feedback** → Reviewers can focus on specific features
- **Faster approval** → Small PRs get reviewed quicker
- **Higher quality** → More attention to each change

### **For Teams**
- **Consistent workflow** → Everyone follows same pattern
- **Cleaner history** → Logical progression of features
- **Better collaboration** → Clear branch relationships
- **Reduced CI time** → Smaller changesets build faster

## 🔧 **Configuration**

### **Default Values**
```kotlin
changeThreshold = 400      // Lines that trigger dialog
cooldownMs = 10000        // 10 seconds before re-trigger
incrementLines = 100      // Threshold increase amount
```

### **Branch Naming Conventions**
- **From master**: `repo-master-username-1`, `repo-master-username-2`
- **Chain increment**: `feature-auth-1`, `feature-auth-2`, `feature-auth-3`
- **Auto-detection**: Automatically finds parent branches

## 🏗 **Architecture**

### **Clean Separation**
```
├── services/
│   ├── GitService.kt          # Git operations
│   └── PRChainerService.kt    # Business logic
├── ui/
│   └── PRChainerDialogs.kt    # User interface
├── listeners/
│   └── LineChangeListener.kt  # Change detection
├── startup/
│   └── PRChainerStartupActivity.kt  # Plugin initialization
└── utils/
    └── NotificationUtils.kt   # Messaging system
```

### **Key Technologies**
- **IntelliJ Platform SDK** → IDE integration
- **Kotlin** → Modern, concise language
- **Swing/JBComponents** → Native IDE UI
- **Git CLI** → Direct Git command execution
- **SVG Icons** → Theme-aware graphics

## 🎯 **Supported IDEs**

- ✅ **IntelliJ IDEA** (Community & Ultimate)
- ✅ **GoLand**
- ✅ **PhpStorm**
- ✅ **WebStorm**
- ✅ **PyCharm**
- ✅ **RubyMine**
- ✅ **CLion**
- ✅ **DataGrip**
- ✅ **All JetBrains IDEs** with platform support

## 🐛 **Troubleshooting**

### **Plugin Not Triggering?**
1. Ensure you're in a Git repository
2. Check if you have uncommitted changes
3. Verify threshold is exceeded (400+ lines)
4. Check IDE logs: `Help` → `Show Log in Explorer`

### **Git Commands Failing?**
1. Ensure Git is installed and in PATH
2. Check repository has proper Git configuration
3. Verify you have push permissions for remote origin
4. Check network connectivity for push operations

### **Dialog Not Appearing?**
1. Check if you're in cooldown period (10 seconds)
2. Verify changes are detected correctly
3. Look for error messages in IDE notifications
4. Restart IDE if plugin seems unresponsive

## 📊 **Version History**

### **v1.0.0** (Current)
- ✅ Parent branch comparison
- ✅ 400-line threshold management
- ✅ Master branch protection
- ✅ Professional UI with icons
- ✅ Cross-IDE compatibility
- ✅ Robust error handling

### **Future Roadmap**
- 🔄 User-configurable settings
- 📈 Usage analytics and insights
- 🎨 Enhanced UI/UX improvements
- 🔗 Integration with PR platforms (GitHub, GitLab)
- 📋 Commit message templates
- 🏷️ Automatic tagging and versioning

## 🤝 **Contributing**

We welcome contributions! Please see our contributing guidelines:

1. **Fork** the repository
2. **Create** a feature branch
3. **Make** your changes
4. **Test** thoroughly
5. **Submit** a pull request

### **Development Setup**
```bash
git clone https://github.com/yourorg/pr-chainer
cd pr-chainer
./gradlew build
./gradlew runIde  # Test in sandbox environment
```

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙋‍♂️ **Support**

- **Issues**: [GitHub Issues](https://github.com/yourorg/pr-chainer/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourorg/pr-chainer/discussions)
- **Email**: support@yourcompany.com

---

**Made with ❤️ for better code reviews**

*PR Chainer - Because every pull request should tell a story, not write a novel.* 