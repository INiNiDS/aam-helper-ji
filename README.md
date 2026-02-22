# AAM (Abstract Alias Mapping) Support for JetBrains IDEs

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![JetBrains Plugin](https://img.shields.io/badge/JetBrains-Plugin-green.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

A JetBrains IDE plugin (IntelliJ IDEA, RustRover, CLion, PyCharm, etc.) that adds language support for the **AAM (Abstract Alias Mapping)** configuration format.

AAM is a robust and lightweight configuration parser designed for flexible configuration files with references, aliases, and a modular structure.

## 🚀 Features

This plugin makes editing `.aam` files easier and more efficient:

- **Syntax Highlighting**: Proper coloring for keys, values, strings, and comments.
- **Directive Support**: distinct highlighting for special directives like `@import`.
- **Comment Handling**: Recognizes lines starting with `#` as comments.
- **Quote Matching**: Automatic pairing and highlighting for quoted string values.
- **Structure View**: (Planned) Visual hierarchy of keys and imported files.

## 📦 Installation

### Install via Plugin Marketplace
1. Open **Settings/Preferences** in your IDE.
2. Go to **Plugins** > **Marketplace**.
3. Search for `AAM Support`.
4. Click **Install** and restart the IDE.

### Install from Disk
1. Download the latest `.zip` release from the [Releases](../../releases) page.
2. Open **Settings/Preferences** > **Plugins**.
3. Click the ⚙️ icon and select **Install Plugin from Disk...**.
4. Select the downloaded archive and restart the IDE.

## AAM Format Syntax

The AAM format is line-based, supporting simple `key = value` pairs, deep resolution, and circular dependency handling.

```aam
# This is a comment
host = "localhost"
port = 8080

# Import other configuration files
@import "database.aam"

# Deep resolution and aliases
base_path = /var/www
current_path = base_path

# Circular references are handled safely by the parser
loop_a = loop_b
loop_b = loop_a
```

## Building from Source

To build the plugin locally:

    Clone the repository:
    Bash

    git clone [https://github.com/your-username/aam-jetbrains-plugin.git](https://github.com/your-username/aam-jetbrains-plugin.git)

    Build using Gradle:
    Bash

    ./gradlew buildPlugin

    The resulting ZIP file will be located in build/distributions/.

## Related Projects

This plugin is designed to support the AAM language ecosystem. The core parser is written in Rust.

    Rust Crate: aaml (Current version: 1.0.5)

    Capabilities: The core library supports deep recursive lookup (find_deep), bidirectional lookup (find_obj), and configuration merging.

## License

Distributed under the MIT License. See LICENSE for more information.
