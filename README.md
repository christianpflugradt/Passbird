# Passbird #

[![version](https://badgen.net/gitlab/release/christianpflugradt/passbird)](https://gitlab.com/christianpflugradt/passbird/-/tags) [![pipeline status](https://gitlab.com/christianpflugradt/passbird/badges/main/pipeline.svg)](https://gitlab.com/christianpflugradt/passbird/-/commits/main) [![coverage report](https://gitlab.com/christianpflugradt/passbird/badges/main/coverage.svg?job=📊%20test-coverage-report)](https://gitlab.com/christianpflugradt/passbird/-/commits/main) [![license](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Passbird is a terminal based password manager written in Java. It works completely offline and encrypts all passwords using an AES/CBC/PKCS5Padding Cipher initialized with a JCEKS (Java Cryptography Extension KeyStore).

Check the FAQ at the bottom if you are wondering whether Passbird is the right tool for you.

## Downloading Passbird ##
The latest version of Passbird can be downloaded [at my website](https://pflugradts.de/password-manager/).

You may also build Passbird yourself by cloning this project and running the Gradle jar task.

## Running Passbird ##
Passbird runs on all major operating systems and requires only Java version 17 or higher to be installed.

Use the following syntax to run Passbird:

`java -jar passbird.jar config-directory`

`config-directory` must point to an existing directory where you want to store your Password database, the keystore and configuration file. An example configuration directory for Linux may be `/etc/passbird` and for Windows `"c:\\program files\\passbird"`. You must use quotes if your configuration path contains spaces.

## Setup ##
The first time you run Passbird its setup routine will execute. You will then be asked to specify a configuration directory and choose a master Password for the Keystore. If you omit the configuration directory when running Passbird or the specified directory does not contain a `passbird.yml` configuration file or a `passbird.ks` Keystore, setup will also be started. 

The master Password you have chosen must be input everytime you start Passbird in non setup mode. Passbird will terminate if you input the wrong Password three times in a row.

Don't worry if you start setup by accident, just press any key other than `c` and setup will exit without touching anything.

## Configuration ##
Once set up Passbird will create a `passbird.yml` with various configuration parameters. The following parameters are available as of now:

 * `application.password.length` specifies the length of generated Passwords (default: 20 characters)
 * `application.password.specialCharacters` specifies whether generated Passwords have special characters such as !@#$% or not (default: true)
 * `application.password.promptOnRemoval` specifies whether manual confirmation is required when an action would delete a previously saved Password (default: true)
 * `adapter.clipboard.reset.enabled` specifies whether clipboard will be cleared after a Password is copied to clipboard (default: true)
 * `adapter.clipboard.reset.delaySeconds` specifies after how many seconds clipboard will be cleared if clipboard reset is enabled (default: 10)
 * `adapter.keyStore.location` contains path to directory where `passbird.ks` keystore file is stored (no default, specified in setup)
 * `adapter.passwordStore.location` contains path to directory where `passbird.pw` database file is stored (no default, specified in setup)
 * `adapter.passwordStore.verifySignature` terminates Passbird if the Password database signature does not match (default: true)
 * `adapter.passwordStore.verifyChecksum` terminates Passbird if the Password database checksum does not match (default: true)
 * `adapter.userInterface.secureInput` specifies whether your master Password and custom Passwords will use secure input where the characters you input will not be displayed in your terminal (default: true)

You may adjust the configuration parameters to your needs by editing the yml file with a text editor. The configuration file must retain a valid yaml format and set all configuration parameters or Passbird might not be able to read it.

## Usage ##

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
        d[EggId] (discard) removes the Egg entirely from the database
        e (export) exports the Password database in a human readable json format
        i (import) imports Passwords into the database from a json file
        l (list) non parameterized, lists all Eggs in the database
        n (Nests) view available Nests and print Nest specific help
        h (help) prints this help
        q (quit) terminates Passbird application 

Nests are an advanced feature of Passbird. Think of it as categories for your Passwords. You may have a Nest for online shopping and another for social networks, or one for personal and another for work related stuff. Nest related help is available by pressing `n` and then enter:

    n (view) displays current Nest, available Nests and Nest commands
    n0 (switch to default) switches to the default Nest
    n[1-9] (switch) switches to the Nest at the given Nest Slot (between 1 and 9 inclusively)
    n[1-9][EggId] (assign) assigns the Password for that EggId to the specified Nest
    n+[1-9] (create) creates a new Nest at the specified Nest Slot
    [NOT YET IMPLEMENTED] n-[1-9] (discard) discards the Nest at the specified Nest Slot

Passbird updates the physical database file immediately after every action. As of now there is no backup function and each action is irrevocable, so think carefully before you delete or overwrite a stored Password. You might want to back up the Password database file and keystore file from time to time.

Some example inputs and what they do:

`ggitlab` copies the Password for Egg with EggId gitlab to clipboard

`sgitlab` sets a random Password for Egg with EggId gitlab - by default a string of length 20 with digits, lowercase and uppercase letters and special characters; if the EggId is already in use, the existing Password will be overwritten

`vgitlab` prints the Password for Egg with EggId gitlab to the terminal (standard out stream)

`dgitlab` deletes Egg with EggId gitlab including its associated Password

`cgitlab` prompts for a custom Password to be input by the user; the input will be hidden by default. Choose this command if you need a Password for a system that does not support standard Passwords generated by Passbird. You don't need to confirm your input, so I advise you to confirm right away that you've input the Password you intended to.

`rgitlab` prompts for a new EggId that replaces the previous EggId gitlab - the new EggId must not yet exist

`e` exports all Eggs to a file `passbird-export.json` in the directory passed on program start

`i` imports all Eggs from a file `passbird-export.json` expected in the directory passed on program start - please note that this will potentially overwrite all Passwords in your database if there are any matching Eggs!

## Migrating from 2.x.x to 3.x.x

In 2.x.x Passbird was known as PwMan3. As part of the rebranding the file names have changed. Everything else remains compatible:
- `pwman3.jar` has been renamed to `passbird.jar` - if you use a script or alias to start the program, make sure you update it accordingly
- `PwMan3.yml` has been renamed to `passbird.yml`
- `PwMan3.ks` has been renamed to `passbird.ks`
- `PwMan3.pw` has been renamed to `passbird.pw`
- import and export will generate / expect a file named `passbird-export.json` now

## <a name="faq"></a>Frequently Asked Questions ##

### Is Passbird the right tool for me? ###
If you prefer the terminal to a graphical user interface, and you don't need to manage hundreds of Passwords, you might want to give Passbird a chance. Keep in mind that Passbird does not use a hierarchy for managing its Eggs, so you might have a hard time memorizing all those EggId or thinking of good and consistent names if you want to store hundreds of Passwords or also want to store related information such as urls and usernames. Passbird will be able to store such meta information in a future version.

### Is Passbird secure? ###
Short answer: No.

Long answer: I am not a security expert and Passbird has not been reviewed by such. Passbird uses a Keystore and a Cipher to symmetrically encrypt all data using a master Password chosen by the user. All data is generally handled in byte arrays by the application. All user input is read and program output is written as single bytes. Only for copying a Password to clipboard I store it in a string because I know no other way to write to the clipboard using Java. Once sensitive decrypted data has been used, like written to the clipboard, the byte array will be overwritten with random bytes. The Password database is encoded twice (first at the individual Egg level, then again for the whole file), so from analyzing it in a hex editor you should not even be able to tell how many Passwords are currently stored in the database. Passbird is also completely offline. If I were to take a wild guess I would say it is probably reasonably safe.

### Does Passbird support Unicode? ###

No. Many programs don't support unicode characters in Passwords. In fact some don't even allow common special characters like backslashes or spaces. Supporting advanced character sets didn't seem worth it to me, thus Passbird translates every byte into an ascii character. You may input a unicode character which is represented in several bytes and Passbird will interpret it as multiple ascii characters. I advise against doing that. You might have trouble reproducing the correct input and end up with some broken Eggs that you can't delete.

### How do I update Passbird? ###

Passbird uses semantic versioning in the style of *x.y.z* where x is the major version, y the minor version and z the patch level version. Updating to a minor or patch level version is very simple: Just download the jar file and use it. Consult the migration notes in this Readme when upgrading to the next major version. 

### What can I do if I forget my master Password? ###
Without the correct master Password you cannot decrypt your Password database. There is no way to bypass this protection. The only thing you can do is to try and find out your Password via brute force. Passbird terminates after 3 wrong attempts so if the program is still running after 3 guesses, you have guessed correctly. I am not aware of any way to protect against brute force attacks by the way, since an attacker could simply brute force the keystore itself instead of using Passbird to find out the correct Password. The best protection is a secure Password that would take a long time to guess, and to keep your keystore file safe.

### What can I do if I lose the keystore file? ###
Your master Password will allow Passbird to retrieve the encryption key from the keystore. Without the encryption key your Password database cannot be decrypted. If you recreate the keystore with the same master Password, the encryption key will NOT be the same. Losing your keystore file means permanently losing access to your Password database.

### Can I run multiple instances of Passbird? ###
Of course! For example, I use separate Password databases for personal and work related Passwords. You can run them in different terminal windows at the same time and the instances won't be aware of each other.

### Can you add a graphical interface? ###
No. The core characteristic of Passbird is its gui-less nature. If you want a gui, use one of the popular gui-based Password managers such as Keepass.

### Can you implement feature xyz? ###
It depends. If I think that feature is useful, and it fits with Passbird I might implement it. Why don't you open a [Gitlab Issue](https://gitlab.com/christianpflugradt/passbird/-/issues) for it? :-)
