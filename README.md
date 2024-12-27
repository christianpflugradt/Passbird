# Passbird

[![version](https://gitlab.com/christianpflugradt/passbird/-/badges/release.svg)](https://gitlab.com/christianpflugradt/passbird/-/releases) [![pipeline status](https://gitlab.com/christianpflugradt/passbird/badges/main/pipeline.svg)](https://gitlab.com/christianpflugradt/passbird/-/commits/main) [![coverage report](https://gitlab.com/christianpflugradt/passbird/badges/main/coverage.svg?job=📊%20test-coverage-report)](https://gitlab.com/christianpflugradt/passbird/-/commits/main) [![license](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

* [Getting started](#getting-started)
    + [Downloading Passbird](#downloading-passbird)
    + [Running Passbird](#running-passbird)
    + [Setup](#setup)
    + [Tutorial: Your first Password](#tutorial-your-first-password)
* [Configuration](#configuration)
    + [Custom Password Configuration](#custom-password-configuration)
* [Usage](#usage)
    + [General Usage](#general-usage)
    + [Nests](#nests)
    + [Proteins](#proteins)
    + [Memory](#memory)
    + [Custom Passwords](#custom-passwords)
* [Migrating Passbird 4.x.x to Passbird 5.x.x](#migrating-passbird-4xx-to-passbird-5xx)
* [Frequently Asked Questions](#frequently-asked-questions)

Passbird is a lightweight, terminal-based password manager built in Kotlin. It operates entirely offline, securing your passwords with AES-GCM encryption and storing the encryption key in a Java KeyStore (JCEKS).

Unsure if Passbird suits your needs? Consult the [FAQ](#frequently-asked-questions) for guidance.

## Getting started

### Downloading Passbird

Download the latest version of Passbird [from the official website](https://pflugradts.de/password-manager/).

Alternatively, you can build Passbird yourself by cloning the project repository and running the Gradle jar task.

### Running Passbird

Passbird runs on all major operating systems and requires Java 17 or higher.

To start Passbird, use the following command:

`java -jar passbird.jar home-directory`

Replace home-directory with the path to an existing directory where your configuration file is stored. This directory is also used for import/export operations. Examples:
- **Linux**: /etc/passbird
- **Windows**: "C:\Program Files\Passbird"

Note: Use quotes if the path contains spaces.

You may optionally specify an initial Nest (explained in detail later) by adding its Nest Slot number (1–9). For example:

`java -jar passbird.jar home-directory 1`

This starts Passbird with the Nest at Slot 1 selected.

### Setup

On first launch, Passbird will guide you through its setup process. You will be prompted to:
1. Specify a home-directory.
2. Create a master password for securing the Java Keystore.

The master password must be entered each time you run Passbird. Entering the incorrect password three times will terminate the program.

Tip: If setup is triggered accidentally, press any key other than c to exit without making changes.

### Tutorial: Your first Password

#### 1. Create a Password

After setup, start Passbird and type: `semail`

This creates a secure password for the identifier email. In Passbird, such identifiers are called EggIds, as passwords are metaphorically stored in Eggs.

#### 2. Copy the Password to Clipboard

Type: `gemail`

The password is now copied to your clipboard. By default, the clipboard is cleared after 10 seconds. You can adjust this timeout or disable clipboard clearing in the configuration file (discussed later).

#### 3. View the Password

To display the password in the terminal, type: `vemail`

#### 4. Delete the Password

Since this is just a tutorial, delete the password by typing: `demail`

#### Summary of Commands Used

In this tutorial, you’ve used these basic commands, all of which operate on an EggId:
1. set: Creates or updates a password for the specified EggId.
2. get: Copies the password for the EggId to the clipboard.
3. view: Displays the password in the terminal.
4. delete: Removes the password and its associated Egg.

Finally, type: `q`

to quit Passbird.

## Configuration

After completing setup, Passbird generates a configuration file named `passbird.yml`. This file contains customizable parameters, structured as shown below:

```yaml
application:
  backup:
    location: backups
    numberOfBackups: 10
    configuration:
      enabled: true
    keyStore:
      enabled: true
    passwordTree:
      enabled: true
  exchange:
    promptOnExportFile: true
  inactivityLimit:
    enabled: false
    limitInMinutes: 10
  password:
    length: 20
    specialCharacters: true
    promptOnRemoval: true
    customPasswordConfigurations:
adapter:
  clipboard:
    reset:
      enabled: false
      delaySeconds: 10
  keyStore:
    location:
  passwordTree:
    location:
    verifyChecksum: true
    verifySignature: true
  userInterface:
    ansiEscapeCodes:
      enabled: false
    audibleBell: false
    secureInput: true
domain:
  eggIdMemory:
    enabled: true
    persisted: false
  protein:
    secureProteinStructureInput: true
    promptForProteinStructureInputToggle: false
```

You may modify the parameters to suit your needs by editing the YAML file in a text editor. Ensure the file adheres to valid YAML syntax and only includes supported parameters. If you omit parameters, Passbird will revert to defaults.

Note: Default values may change with major updates. New parameters introduced in minor updates are usually inactive by default to preserve the existing user experience.

For a complete list of configuration settings and their descriptions, consult the [CONFIGURATION.md](CONFIGURATION.md) file. It provides an exhaustive reference to all available parameters, their purposes, and how to customize them.

### Custom Password Configuration

Passbird supports up to nine custom password configurations for specific use cases (e.g., PINs or highly secure passwords). Each configuration has six customizable properties:
1. name: A descriptive name for the configuration.
2. length: The length of the generated password.
3. hasNumbers: Include numeric characters.
4. hasLowercaseLetters: Include lowercase letters.
5. hasUppercaseLetters: Include uppercase letters.
6. hasSpecialCharacters: Include non-alphanumeric characters (e.g., !@#$%^&*).
7. unusedSpecialCharacters: Specify any special characters to exclude.

```yaml
customPasswordConfigurations:
    - name: example with default settings
      length: 20
      hasNumbers: true
      hasLowercaseLetters: true
      hasUppercaseLetters: true
      hasSpecialCharacters: true
      unusedSpecialCharacters: ""
    - name: alphanumeric
      length: 32
      hasSpecialCharacters: false
    - name: only common special characters
      length: 28
      unusedSpecialCharacters: " \":'`&!"
    - name: very secure
      length: 48
    - name: PIN-4
      length: 4
      hasLowercaseLetters: false
      hasUppercaseLetters: false
      hasSpecialCharacters: false
    - name: PIN-5
      length: 5
      hasLowercaseLetters: false
      hasUppercaseLetters: false
      hasSpecialCharacters: false
```

Defining Custom Configurations
- Modify the customPasswordConfigurations section in passbird.yml to define your custom settings.
- If a property is omitted, Passbird uses the default value for that property.

Tip: Custom configurations are especially useful for generating passwords tailored to systems with specific requirements, such as numeric-only PINs.

## Usage

Passbird uses a simple, command-based syntax to manage passwords. Each password is stored in an Egg, identified by a unique EggId.

For example, if you want to store your email account password and choose email as its EggId:
- To create and save a password for email, enter: `semail`

The s command stands for “set” and generates a random password for the specified EggId. If the EggId already exists, the password will be replaced.

- To copy the password to the clipboard, enter: `gemail`

The g command stands for “get” and copies the password to your clipboard.

You can access Passbird’s in-app help at any time by pressing h and then Enter.

    Usage: [command][parameter]
    A command takes at most one parameter which is usually an EggId.

    commands:
        g[EggId] (get)        Copies the password for the specified Egg to the clipboard.
        s[EggId] (set)        Sets a random password for the specified Egg, overwriting any existing one.
        c[EggId] (custom set) Prompts the user to input a custom password for the specified Egg.
        v[EggId] (view)       Displays the password for the specified Egg in the console.
        r[EggId] (rename)     Renames the specified Egg by prompting the user for a new EggId.
        d[EggId] (discard)    Deletes the specified Egg and its associated password.

        e (export)            Exports the Password Tree to a human-readable JSON file.
        i (import)            Imports passwords from a JSON file into the Password Tree.
        l (list)              Lists all Eggs in the current Nest.
        h (help)              Displays this help menu.
        q (quit)              Exits the Passbird application.

        n (Nests)             Displays available Nests and related commands.
        p? (Proteins)         Displays Protein-related usage information.
        s? (Password configs) Displays available password configurations and related help.

### General Usage

Passbird immediately updates the password database (Password Tree file) after every action. Below are some example commands using the email EggId:

`gemail` copies the password for the Egg identified by email to the clipboard.

`semail` sets a random password for the Egg identified by email. By default, the password is 20 characters long and includes digits, lowercase and uppercase letters, and special characters. If the EggId already exists, the existing password will be replaced.

`vemail` displays the password for the Egg identified by email in the terminal (standard output).

`demail` deletes the Egg identified by email, including its associated password.

`cemail` prompts you to input a custom password for the Egg identified by email. The input is hidden by default. Use this command if the system you’re storing the password for does not support Passbird’s standard password format. Be sure to verify your input immediately, as it is not confirmed.

`remail` prompts you to rename the Egg identified by email. The new EggId must be unique (not already in use).

`e` exports all Eggs to a file named `passbird-export.json` in the directory specified during program start.

`i` imports all Eggs from a `passbird-export.json` file located in the directory specified during program start.

### Nests

Nests are an advanced feature in Passbird, allowing you to organize passwords into categories. For example, you might create one Nest for online shopping, another for social networks, or separate Nests for personal and work-related accounts. To access Nest-specific help, press n followed by Enter.

    n (view)               Displays the current Nest, available Nests, and related commands.
    n0 (switch)            Switches to the default Nest.
    n[1-9] (switch)        Switches to the Nest in the specified Nest Slot (1–9).
    n[0-9][EggId] (assign) Assigns the specified EggId to the Nest in the given Nest Slot.
    n+[1-9] (create)       Creates a new Nest in the specified Nest Slot.
    n-[1-9] (discard)      Deletes the Nest in the specified Nest Slot.

### Proteins

Proteins allow you to store additional information alongside your passwords. While an Egg contains a password and its descriptive EggId, Proteins let you add up to ten extra data entries to the same Egg. Each Protein has a Type, which describes the kind of information (e.g., “user,” “URL,” or “recovery code”), and a Structure, which holds the actual data (e.g., “john.doe”).

To view commands related to Proteins, input `p?` and press Enter.

    p? (help)                Displays this help menu for Protein commands.
    p[EggId] (info)          Displays the Protein Types associated with the specified Egg.
    p*[EggId] (details)      Displays both the Protein Types and their Structures for the specified Egg.
    p[0-9][EggId] (copy)     Copies the Protein Structure in the specified Slot (0–9) to the clipboard.
    p+[0-9][EggId] (update)  Updates the Protein Structure and optionally the Type in the specified Slot.
    p-[0-9][EggId] (discard) Deletes the Protein Structure and Type from the specified Slot.

Consider an Egg identified by the EggId email. To view the Proteins associated with this Egg, input `pemail`. This command displays the Protein Types (e.g., “user,” “URL,” “recovery code”) without revealing the sensitive Structures. For instance, it might indicate that the Egg contains a user, a URL, and a recovery code.

To display the actual Protein Structures (sensitive data) alongside their Types, use `p*email`. This command functions similarly to `pemail`, but it additionally reveals the full content of the Structures.

If the Structure of a specific Protein is required in the clipboard, determine its Slot number (e.g., Slot 2 as shown in the `pemail` output) and use `p2email`. This will copy the content of Slot 2 directly to the clipboard.

Each Egg contains ten Protein Slots, which are initially empty. To create or update a Protein in a specific Slot, e.g. Slot 1, use `p+1email`. You will first be prompted to enter the Protein Type (e.g., “user”). If a Protein already exists in the specified Slot and no new Type is provided, the existing Type will remain unchanged. Next, you will be asked to input the Protein Structure. Pressing Enter without input will abort the creation or update process.

To delete a Protein from a specific Slot, e.g. Slot 1, input `p-1email`. This command removes both the Type and Structure from the specified Slot.

### Memory

The EggIdMemory stores up to ten of the most recently accessed EggIds for each Nest. This feature allows you to avoid retyping the same EggId repeatedly, which is particularly useful when performing multiple operations on the same Egg, such as setting multiple Proteins.

To view commands related to the EggIdMemory, input `m?`. This displays the following options:

    m? (help)           Displays this help menu for Memory commands.
    m (info)            Lists the EggIds currently stored in the EggIdMemory.
    m[0-9] (copy)       Copies the EggId from the specified Memory Slot to the clipboard.
    m[0-9]Command (use) Executes the specified command using the EggId from the given Memory Slot.

The `use memory` command is especially versatile. Consider an Egg identified by email. To copy its password to the clipboard, you would first input `gemail`. This action stores the EggId email in the most recent memory slot (Slot 0).

Subsequently, if you wish to set a Protein for this Egg, you could input `p+1email` directly. However, by using the memory feature, you may instead input `m0p+1`. This command invokes the `p+1` operation using the EggId stored in Slot 0, eliminating the need to retype "email".

The memory feature extends to other operations as well. For example:
- To rename the second most recently accessed EggId (stored in Slot 1), input `m1r`.
- To view the password for the third most recently accessed EggId (stored in Slot 2), input `m2v`.

At any time, you can input `m` to display the contents of the EggIdMemory. This command lists the EggIds currently stored in memory, ordered from most recently used (Slot 0) to least recently used (Slot 9).

### Custom Passwords

Custom passwords are an advanced feature briefly introduced in the configuration section. Passbird allows you to define up to nine custom password configurations for specific use cases. These configurations can be applied using the set command by specifying the corresponding configuration index.

The command `semail` is interchangeable with `s0email`, which uses the default configuration. For custom configurations, use commands `s1email` through `s9email` to apply the respective custom settings, provided they have been defined in the configuration file.

To view all available configurations and their indices, input `s?`. Using the example configuration provided earlier, the output might appear as follows:

    0: Default
        20 characters
    1: example with default settings
        20 characters
    2: alphanumeric
        32 characters
        no special characters
    3: only common special characters
        28 characters
        unused special characters:  ":'`&!
    4: very secure
        48 characters
    5: PIN-4
        4 characters
        no letters
        no special characters
    6: PIN-5
        5 characters
        no letters
        no special characters
    
    Available Set Commands:

        s? (help)                  Displays an overview of available password configurations.
	    s[EggId] (set)             Sets a random password for the specified EggId using the default configuration.
        s[1-9][EggId] (set custom) Sets a random password for the specified EggId using a custom configuration.

## Migrating Passbird 4.x.x to Passbird 5.x.x

With the release of Passbird 5.x.x, the Tree structure has been updated to use AES-GCM encryption, a method recommended by NIST for enhanced security. Additionally, the file structure has been modified to prepare for upcoming features related to shortcuts. Due to these changes, migrating from Passbird 4.x.x requires exporting and reimporting your passwords.

Migration Steps:
1.	Export Your Passwords:
Using Passbird 4.x.x, export all stored passwords by executing the export command `e`. The export will preserve Nests, Proteins, and other data, ensuring no loss of information. However, it is strongly recommended to manually back up the entire Passbird folder as an additional safeguard.
2.	Prepare for Update:
After exporting, terminate Passbird and delete the password database file named passbird.tree. This file is no longer compatible with the updated format.
3.	Update Passbird:
Replace your Passbird installation with version 5.x.x and start the program. An error will appear indicating that the password database cannot be read. This is expected and can be safely ignored.
4.	Import Your Passwords:
Execute the import command `i` to restore your data. This process will reimport all Nests, Passwords, and Proteins into the updated format.

## Frequently Asked Questions

### Is Passbird the right tool for me?
If you prefer the terminal to a graphical user interface and do not need to manage hundreds of passwords, Passbird might be a good choice for you. Note that Passbird does not use a hierarchical system for managing Eggs. As such, if you need to store hundreds of passwords, or if you require a detailed structure for related information like URLs or usernames, you may find it challenging to organize your data. 

### Is Passbird secure?
Passbird has not been reviewed by security experts. It uses a Keystore and a Cipher to encrypt all data symmetrically with a master password chosen by the user. Sensitive data is generally handled as byte arrays, and decrypted data is immediately overwritten after use. The password database is doubly encoded, both at the individual Egg level and for the entire file, making it difficult to determine even the number of stored passwords from a hex editor. Passbird operates entirely offline. While these measures likely offer reasonable security, they are not guaranteed.

### Does Passbird support Unicode?
No. Many programs do not support Unicode characters in passwords, and some even restrict special characters like backslashes or spaces. Passbird translates every byte into an ASCII character. Inputting Unicode characters will result in multiple ASCII characters, which may cause inconsistencies and make certain Eggs undeletable. It is advised to avoid Unicode inputs.

### How do I update Passbird?
Passbird follows semantic versioning (*x.y.z*, where `x` is the major version, `y` the minor version, and `z` the patch level). To update to a minor or patch version, download the latest JAR file and use it as usual. For major updates, consult the migration notes provided in this README.

You can find the ten most recent versions of Passbird [on the official website](https://pflugradts.de/password-manager/), along with checksums for verifying file integrity using tools like `md5sum`. For an automated but less secure update process, consider [Passbird-Updater](https://github.com/christianpflugradt/passbird-updater), though manual updates are recommended.

### What happens if I lose my master password or keystore file?
Losing your master password or keystore file results in permanent data loss. The master password is required to decrypt the database, and the keystore file stores the encryption key. Even recreating the keystore with the same master password will not regenerate the same encryption key.

If you forget your master password, your only recourse is brute force guessing.

### How do I back up my passwords?
Passbird supports file-based backups for the password database (`passbird.tree`), configuration file (`passbird.yml`), and keystore file (`passbird.sec`). These can be automatically managed through the backup settings in `passbird.yml`. Refer to the [configuration section](./CONFIGURATION.md) for details on enabling and customizing backups.

While Passbird does not offer built-in online backup features, you can securely store these backup files using external tools like Nextcloud, Dropbox, or similar cloud services. Ensure that the storage location of your backups is well-protected, as these files contain sensitive data necessary for password recovery.

### Can Passbird integrate with browsers or other tools?
No. Passbird is designed with a 100% offline philosophy, ensuring maximum privacy and security. It does not integrate with browsers, plugins, or other tools. For users requiring browser integration, a different password manager may be more suitable.

### Can I run multiple instances of Passbird?
Yes. For example, you might use separate databases for personal and work-related passwords. These can be run simultaneously in different terminal windows, as the instances operate independently.
