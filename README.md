# Passbird

[![version](https://gitlab.com/christianpflugradt/passbird/-/badges/release.svg)](https://gitlab.com/christianpflugradt/passbird/-/releases) [![pipeline status](https://gitlab.com/christianpflugradt/passbird/badges/main/pipeline.svg)](https://gitlab.com/christianpflugradt/passbird/-/commits/main) [![coverage report](https://gitlab.com/christianpflugradt/passbird/badges/main/coverage.svg?job=📊%20test-coverage-report)](https://gitlab.com/christianpflugradt/passbird/-/commits/main) [![license](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Passbird is a terminal based password manager written in Kotlin. It works completely offline and encrypts all passwords using an AES/GCM Cipher and an encryption key stored in Java KeyStore (JCEKS).

Check the FAQ at the bottom if you are wondering whether Passbird is the right tool for you.

## Downloading Passbird
The latest version of Passbird can be downloaded [at my website](https://pflugradts.de/password-manager/).

You may also build Passbird yourself by cloning this project and running the Gradle jar task.

## Running Passbird
Passbird runs on all major operating systems and requires only Java version 17 or higher to be installed.

Use the following syntax to run Passbird:

`java -jar passbird.jar home-directory`

`home-directory` must point to an existing directory where your configuration file is stored and the import and export commands read from respectively write to. An example configuration directory for Linux may be `/etc/passbird` and for Windows `"c:\\program files\\passbird"`. You must use quotes if your configuration path contains spaces.

You may specify the initial Nest as a second optional argument. Nests are an advanced feature explained further below. Nests are specified by their Nest Slot ranging from 1 to 9. Any other value results in the default Nest being initially selected.

The following example starts Passbird with Nest at Nest Slot 1 as the initial Nest: `java -jar pasbird.jar home-directory 1`

## Setup
The first time you run Passbird its setup routine will execute. You will then be asked to specify a home directory and choose a master Password for the Keystore.

The master Password you have chosen must be input everytime you start Passbird in non setup mode. Passbird will terminate if you input the wrong Password three times in a row.

Don't worry if you start setup by accident, just press any key other than `c` and setup will exit without touching anything.

## Configuration
Once set up Passbird will create a `passbird.yml` with various configuration parameters. The following structure represents the default in the latest release of Passbird, enriched by explanatory comments:

```yaml
application:
  # manages backups of important Passbird files
  backup:
    # default location for all backups, can be relative or absolute path
    location: backups
    # default number of backups to keep per file
    numberOfBackups: 10
    # manages backups for passbird.yml configuration file
    configuration:
      # enables backups for this file
      enabled: true
      # overrides default location for this file
      location:
      # overrides default number of backups for this file
      numberOfBackups:
    # manages backups for passbird.sec keystore file
    keyStore:
      # enables backups for this file
      enabled: true
      # overrides default location for this file
      location:
      # overrides default number of backups for this file
      numberOfBackups:
    # manages backups for passbird.tree (database file)
    passwordTree:
      # enables backups for this file
      enabled: true
      # overrides default location for this file
      location:
      # overrides default number of backups for this file
      numberOfBackups:
  exchange:
    # prompts for deletion if password export file is detected on program start
    promptOnExportFile: true
  inactivityLimit:
    # terminates Passbird after a period of inactivity if enabled
    enabled: false
    # required minutes of continuous inactivity to pass before termination
    limitInMinutes: 10
  password:
    # length of passwords generated through Passbird
    length: 20
    # enables non-alphanumeric characters in passwords such as !@#$^&.)"\
    specialCharacters: true
    # requires confirmation before actions that delete or update passwords
    promptOnRemoval: true
    # custom password configurations are empty by default and explained separately
    customPasswordConfigurations:
adapter:
  clipboard:
    reset:
      # empties clipboard after a password is copied to it
      enabled: true
      # number of seconds to pass before clipboard is emptied
      delaySeconds: 10
  keyStore:
    # path to directory where keystore is located
    location:
  passwordTree:
    # path to directory where Password Tree is located
    location:
    # terminates Passbird if Password Tree checksum doesn't meet expectations
    verifyChecksum: true
    # terminates Passbird if Password Tree signature doesn't meet expectations
    verifySignature: true
  # some of these settings are not supported by all terminals
  userInterface:
    ansiEscapeCodes:
      # enables colorful mode
      enabled: true 
    # gives acoustic feedback on invalid input
    audibleBell: false
    # hides sensitive user input such as passwords
    secureInput: true
domain:
    eggIdMemory:
        # memorizes the up to ten most recently used egg ids per nest for memory related usage
        enabled: true
        # persists the memorized egg ids across Passbird restarts
        persisted: true
    protein:
        # treats protein structures as secure input
        secureProteinStructureInput: true
        # if enabled prompts to toggle secureProteinStructureInput setting for next input
        promptForProteinStructureInputToggle: false
```
You may adjust the configuration parameters to your needs by editing the yaml file with a text editor. The configuration file must retain a valid yaml format and may only contain parameters known to Passbird. While not advised it is possible to omit most parameters in which case the defaults will be used.

Defaults are subject to change in major updates of Passbird. When new parameters are introduced in minor updates they're usually inactive by default to change the experience as little as possible.

### Custom Password Configuration

You may define up to nine custom password configurations for specific use cases such as PINs for smartphones and cash cards which are often limited to 4-6 digits. A custom configuration has six properties. As they have default values, it's only necessary to specify them, when they deviate from their defaults. The following configuration section defines six custom password configurations of which the first is equivalent to the default:

```yaml
    customPasswordConfigurations:
       # each hyphen starts a new custom password configuration
       - name: example with default settings
         length: 20
         # if all "has" properties are false the password will consistent
         # of only numbers as a fallback
         hasNumbers: true
         hasLowercaseLetters: true
         hasUppercaseLetters: true
         hasSpecialCharacters: true
         # all special characters are used by default
         unusedSpecialCharacters: ""
       - name: alphanumeric
         length: 32
         hasSpecialCharacters: false
       - name: only common special characters
         length: 28
         # unused characters should be specified in quotes
         # the quote character itself must be preceded by a backslash
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

## Usage

Passbird has a very simple command based usage syntax. Each Password is stored in an Egg, and you must set and memorize an identifier for each, which is known as its `EggId`.

Let's say you want to store your GitLab Password and choose `gitlab` as its EggId. To create and save a Password for EggId `gitlab` you will input `sgitlab` and press enter. `s` stands for the `set` command which sets a Password for an Egg. If the EggId already exists, it will set a new Password for that Egg. To copy the Password to clipboard you must input `ggitlab`. This is the `get` command represented by the character `g`.

The general usage info will be given below and is also available in Passbird by pressing `h` and then enter:

    Usage: [command][parameter]
    A command takes at most one parameter which is usually an EggId.

    commands:
        g[EggId] (get) copies the Password contained in that Egg to clipboard
        s[EggId] (set) sets a random Password for this Egg overwriting any that existed
        c[EggId] (custom set) like set but prompts the user to input a new Password
        v[EggId] (view) prints the Password contained in that Egg to the console
        r[EggId] (rename) renames an Egg by prompting the user for a new one
        d[EggId] (discard) removes the Egg entirely from the Tree

        e (export) exports the Password Tree in a human readable json format
        i (import) imports Passwords into the Tree from a json file
        l (list) non parameterized, lists all Eggs in the current Nest
        h (help) prints this help
        q (quit) terminates Passbird application 

        n (Nests) prints available Nests and Nest specific help
        p? (Proteins) prints protein usage information
        s? (Password configurations) prints available configurations and set specific help

### General Usage

Passbird updates the physical Password Tree (file) immediately after every action.

Some example inputs and what they do:

`ggitlab` copies the Password for Egg with EggId gitlab to clipboard

`sgitlab` sets a random Password for Egg with EggId gitlab - by default a string of length 20 with digits, lowercase and uppercase letters and special characters; if the EggId is already in use, the existing Password will be overwritten

`vgitlab` prints the Password for Egg with EggId gitlab to the terminal (standard out stream)

`dgitlab` deletes Egg with EggId gitlab including its associated Password

`cgitlab` prompts for a custom Password to be input by the user; the input will be hidden by default. Choose this command if you need a Password for a system that does not support standard Passwords generated by Passbird. You don't need to confirm your input, so I advise you to confirm right away that you've input the Password you intended to.

`rgitlab` prompts for a new EggId that replaces the previous EggId gitlab - the new EggId must not yet exist

`e` exports all Eggs to a file `passbird-export.json` in the directory passed on program start

`i` imports all Eggs from a file `passbird-export.json` expected in the directory passed on program start.

### Nests

Nests are an advanced feature of Passbird. Think of it as categories for your Passwords. You may have a Nest for online shopping and another for social networks, or one for personal and another for work related stuff. Nest related help is available by pressing `n` and then enter:

    n (view) prints current Nest, available Nests and Nest commands
    n0 (switch to default) switches to the default Nest
    n[1-9] (switch) switches to the Nest at the given Nest Slot (between 1 and 9 inclusively)
    n[1-9][EggId] (assign) assigns the Password for that EggId to the specified Nest
    n+[1-9] (create) creates a new Nest at the specified Nest Slot
    n-[1-9] (discard) discards the Nest at the specified Nest Slot

### Proteins

Proteins can store additional information for your Passwords. An Egg contains a Password and a descriptive EggId. Proteins offer you to store up to ten additional pieces of information in that Egg. Each Protein has a Type and a Structure. The Type describes the kind of information, be it a user, a URL or a recovery code. The Structure contains the actual information. So a Protein could have a Type "user" and a Structure "john.doe".

Proteins introduce a few new commands which can be listed by inputting `p?` and then enter:

    p? (help) prints this help
    p[EggId] (info) prints Protein Types for specified Egg
    p*[EggId] (complete info) prints Protein Types and Structures for specified Egg
    p[0-9][EggId] (copy) copies the Protein Structure to clipboard
    p+[1-9][EggId] (update) updates the Protein Structure and optionally Type as well
    p-[1-9][EggId] (discard) discards the Protein Structure and Type

Let's say we have an Egg identified by EggId `gitlab`. You can input `pgitlab` to safely view its Proteins only revealing the Types which are deemed non-sensitive. That way you could see that alongside the Password you've also stored the user, the URL to that service and a recovery code. For each Protein you'll also see the Slot you assigned which you need to copy the Structure into the clipboard.

If you want to print the actual user, URL and so on, you can input `p*gitlab`. This will print potentially sensitive information on the command line. This command behaves exactly the same as `pgitlab` except it won´t mask the sensitive Structures but print their actual content.

If you want to copy the user for a corresponding Password into clipboard, and you know it's stored in Slot 2 because you've printed the Protein information with `pgitlab`, you can input `p2gitlab` to have it copied to your clipboard.

Each Egg has always ten, initially empty Protein Slots. To create a Protein and insert it into a Slot, you can use the update command. This command will first prompt you to enter a Protein Type. If the Protein already exists, and you just press enter, the existing Type will be kept. Then you'll be asked to input the Protein Structure. You can just press enter to abort Protein creation.

A command to delete existing Proteins will be implemented in the near future.

### Custom Passwords

Custom passwords are another advanced feature briefly covered in the configuration section. You may define up to nine of these and can apply them by using the set command while referring to their specific index. While `seggid` is interchangeable with `s0eggId` the commands `s1eggid` up to `s9eggid` will use the respective configuration if defined. You may view all available configurations and their indices by inputting `s?`. Using the example configuration above the output will look like this:

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

	    s[EggId] sets a random Password with default configuration
        s[1-9][EggId] sets a random Password with specified configuration
        s? prints this overview

## Migrating Passbird 4.x.x to Passbird 5.x.x

The Tree structure has changed as Passbird 5.x.x is now encrypted using AES-GCM, an encryption algorithm recommended by NIST. Furthermore, the structure of the file has slightly changed in preparation of an upcoming feature related to shortcuts.For that reason, using Passbird 4.x.x, all Passwords must be exported using the `export` command (letter `e`). The export will preserve Nests and Proteins, so no data should get lost in the process. Nevertheless, it is recommended to manually back up the folder where Passbird resides in case something goes wrong.

After creating an export file, terminate Passbird and delete the password database file named `passbird.tree`. Now update Passbird to 5.x.x and start the program. Passbird will output an error because the password database cannot be read. Ignore this error and run the `import` command (letter `i`), which will restore the database and all Nests, Passwords and Proteins.

## <a name="faq"></a>Frequently Asked Questions

### Is Passbird the right tool for me?
If you prefer the terminal to a graphical user interface, and you don't need to manage hundreds of Passwords, you might want to give Passbird a chance. Keep in mind that Passbird does not use a hierarchy for managing its Eggs, so you might have a hard time memorizing all those EggId or thinking of good and consistent names if you want to store hundreds of Passwords or also want to store related information such as urls and usernames. Passbird will be able to store such meta information in a future version.

### Is Passbird secure?
Short answer: No.

Long answer: I am not a security expert and Passbird has not been reviewed by such. Passbird uses a Keystore and a Cipher to symmetrically encrypt all data using a master Password chosen by the user. All data is generally handled in byte arrays by the application. All user input is read and program output is written as single bytes. Only for copying a Password to clipboard I store it in a string because I know no other way to write to the clipboard using Java. Once sensitive decrypted data has been used, like written to the clipboard, the byte array will be overwritten with random bytes. The Password database is encoded at the individual Egg level and then again for the whole file, so from analyzing it in a hex editor you should not even be able to tell how many Passwords are currently stored in it. Passbird is also completely offline. If I were to take a wild guess I would say it is probably reasonably safe.

### Does Passbird support unicode?

No. Many programs don't support unicode characters in Passwords. In fact some don't even allow common special characters like backslashes or spaces. Supporting advanced character sets didn't seem worth it to me, thus Passbird translates every byte into an ascii character. You may input a unicode character which is represented in several bytes and Passbird will interpret it as multiple ascii characters. I advise against doing that. You might have trouble reproducing the correct input and end up with some broken Eggs that you can't delete.

### How do I update Passbird?

Passbird uses semantic versioning in the style of *x.y.z* where x is the major version, y the minor version and z the patch level version. Updating to a minor or patch level version is very simple: Just download the jar file and use it. Consult the migration notes in this Readme when upgrading to the next major version. 

You will always find the ten most recent versions of Passbird [at my website](https://pflugradts.de/password-manager/). There you will also find the checksum for each jar file. You can use the program `md5sum`, pre-installed on many operating systems, to verify file integrity: `md5sum filename`

A more automated but less secure alternative to manually updating Passbird is [Passbird-Updater](https://gitlab.com/christianpflugradt/passbird-updater). Please consult its [README](https://gitlab.com/christianpflugradt/passbird-updater#passbird-updater) before using it. Passbird Updater is not part of Passbird and the recommended way is to manually update Passbird.

### What can I do if I forget my master Password?
Without the correct master Password you cannot decrypt your Password database. There is no way to bypass this protection. The only thing you can do is to try and find out your Password via brute force. Passbird terminates after 3 wrong attempts so if the program is still running after 3 guesses, you have guessed correctly. I am not aware of any way to protect against brute force attacks by the way, since an attacker could simply brute force the keystore itself instead of using Passbird to find out the correct Password. The best protection is a secure Password that would take a long time to guess, and to keep your keystore file safe.

### What can I do if I lose the keystore file?
Your master Password will allow Passbird to retrieve the encryption key from the keystore. Without the encryption key your Password database cannot be decrypted. If you recreate the keystore with the same master Password, the encryption key will NOT be the same. Losing your keystore file means permanently losing access to your Password database.

### Can I run multiple instances of Passbird?
Of course! For example, I use separate Password databases for personal and work related Passwords. You can run them in different terminal windows at the same time and the instances won't be aware of each other.

### Can you add a graphical interface?
No. The core characteristic of Passbird is its gui-less nature. If you want a gui, use one of the popular gui-based Password managers such as Keepass.

### Can you implement feature xyz?
It depends. If I think that feature is useful, and it fits with Passbird I might implement it. Why don't you open a [GitHub Issue](https://gitlhub.com/christianpflugradt/passbird/issues) for it? :-)
