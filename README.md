# PwMan3 #

[![pipeline status](https://gitlab.com/christianpflugradt/pwman3/badges/master/pipeline.svg)](https://gitlab.com/christianpflugradt/pwman3/-/commits/master) [![coverage report](https://gitlab.com/christianpflugradt/pwman3/badges/master/coverage.svg)](https://gitlab.com/christianpflugradt/pwman3/-/commits/master) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

PwMan3 is a terminal based password manager written in Java. It works completely offline and encrypts all passwords using a AES/CBC/PKCS5Padding Cipher initialized with a JCEKS (Java Cryptography Extension KeyStore).

Check the FAQ at the bottom if you are wondering whether PwMan3 is the right tool for you.

## Downloading PwMan3 ##
The latest version of PwMan3 can be downloaded [at my website](https://pflugradts.de/tools/pwman3 "get PwMan3").

You may also choose to build PwMan3 yourself by cloning this project and running the jar task with Gradle.

## Running PwMan3 ##
PwMan3 runs on all major operating systems and requires only Java version 11 or higher to run.

Use the following syntax to run PwMan3:

java -jar pwman3.jar configDirectory

configDirectory must point to an existing directory where you want to store your password database, the keystore and configuration file. An example configurationDirectory for Linux may be */etc/pwman3* and for Windows *"c:\\program files\\pwman3"*. You must use quotes if your configuration path contains spaces.

## Setup ##
The first time you run PwMan3 its setup routine will execute. You will then be asked to to specify a configDirectory and choose a master password for the Keystore. If you omit the configDirectory when running PwMan3 or the specified directory does not contain a PwMan3.yml configuration file or a PwMan3.ks Keystore, setup will also be started. 

The master password you have chosen must be input everytime you start PwMan3 in non setup mode. PwMan3 will terminate if you input the wrong password three times in a row.

Don't worry if you start setup by accident, just press any key other than *c* and setup will exit without touching anything.

## Configuration ##
Once set up PwMan3 will create a PwMan3.yml with various configuration parameters. The following parameters are available as of now:

 * **application.password.length**: specifies the length of generated passwords (default: 20 characters)
 * **application.password.specialCharacters**: specifies whether generated passwords have special characters such as !@#$% or not (default: true)
 * **application.password.promptOnRemoval**: not yet functional
 * **adapter.clipboard.reset.enabled**: specifies whether clipboard will be cleared after a password is copied to clipboard (default: true)
 * **adapter.clipboard.reset.delaySeconds**: specifies after how many seconds clipboard will be cleared if clipboard reset is enabled (default: 10)
 * **adapter.keyStore.location**: contains path to directory where PwMan3.ks keystore file is stored (no default, specified in setup)
 * **adapter.passwordStore.location**: contains path to directory where PwMan3.pw database file is stored (no default, specified in setup)
 * **adapter.passwordStore.verifySignature**: not yet functional
 * **adapter.passwordStore.verifyChecksum**: not yet functional
 * **adapter.userInterface.secureInput**: specifies whether your master password and custom passwords will use secure input where the characters you input will not be displayed in your terminal (default: true)

You may adjust the configuration parameters to your needs by editing the yml file with a text editor. The configuration file must retain a valid yaml format and set all configuration parameters or PwMan3 might not be able to read it.

## Usage ##

PwMan3 has a very simple command based usage syntax. You must memorize a key for each password you want to store. 

Let's say you want to store your Gitlab password and choose *gitlab* as a key. To create and save a password for key *gitlab* you will input *sgitlab* and press enter. *s* stands for the *set* command which sets a password for a key. If the key already exists, it will set a new password for that key. To copy the password to clipboard you must input *ggitlab*. This is the *get* command represented by the character *g*.

The general usage info will be given below and is also available in PwMan3 by pressing *h* and then enter:

    Usage: [command][parameter]
	A command takes at most one parameter which is either a key to a password or an absolute path to a file.
    commands:
        g[key] (get) copies the password for that key to clipboard
        s[key] (set) sets a random password for a key overwriting any that existed
        c[key] (custom set) like set but prompts the user to input a new password
        v[key] (view) prints the password for that key to the console
        d[key] (discard) removes key and password from the database
        e[directory] (export) exports the password database as a human readable json file to the specified directory
        i[directory] (import) imports a json file containing passwords into the database from the specified directory
        l (list) nonparameterized, lists all keys in the database
        h (help) nonparameterized, prints this help
        q (quit) quits pwman3 application

PwMan3 updates the physical database file immediately after every action. As of now there is no backup function and each action is irrevocable, so think carefully before you delete or overwrite a stored password. You might want to backup the password database file and keystore file from time to time.

Some example inputs and what they do:

**input**: ggitlab **=> action**: will copy password for key gitlab to clipboard

**input**: sgitlab **=> action**: will set a random password for key gitlab - by default a string of length 20 with digits, lowercase and uppercase letters and special characters; if the key already exists the existing password will be overwritten

**input**: vgitlab **=> action**: will print the password for key gitlab to the terminal (standardOut)

**input**: dgitlab **=> action**: will delete key gitlab and its associated password

**input**: cgitlab **=> action**: will prompt for a custom password to be input by the user; the input will be hidden by default. Choose this command if you need a password for a system that does not support standard passwords generated by PwMan3. You don't need to confirm your input, so I advice you to confirm right away that you've input the password you intended to.

**input**: e/tmp **=> action**: exports all keys and their associated passwords to a file /tmp/PwMan3.json (example for a valid Linux path)

**input**: ic:\ **=> action**: imports all passwords from a file c:\PwMan.json if the file exists (example for a valid Windows path) - please note that this will potentially overwrite all passwords in your database if there are any matching keys!

## <a name="faq"></a>Frequently Asked Questions ##

#### Is PwMan3 the right tool for me? ####
If you prefer the terminal to a graphical user interface and you don't need to store many hundreds of passwords, you might want to give PwMan3 a chance. Keep in mind that PwMan3 does not use a hierarchy for managing its keys so you might have a hard time memorizing all those keys or thinking of good names if you want to store hundreds of passwords or also want to store related information such as urls and usernames.

#### Is PwMan3 secure? ####
Short answer: No.

Long answer: I am not a security expert and PwMan3 has not been reviewed. PwMan3 uses a Keystore and a Cipher to symmetrically encrypt all data using a master password chosen by the user. All data is generally handled in byte arrays by the application. All user input is read and program output is written as single bytes. Only for copying a password to clipboard I store it in a string because I know no other way to write to the clipboard using Java. Once sensitive decrypted data has been used, like written to the clipboard, the byte array will be overwritten with random bytes. The password database is double encoded so from analyzing it in a hex editor you should not even be able to tell how many passwords are currently stored in the database. PwMan3 is also completely offline. If I were to take a wild guess I would say it is probably reasonably safe.

#### What can I do if I forget my master password? ####
Without the correct master password you cannot decrypt your password database. There is no way to bypass this protection. The only thing you can do is to try and find out your password via brute force. PwMan3 terminates after 3 wrong attempts so if the program is still running after 3 guesses, you have guessed correctly. I am not aware of any way to protect against brute force attacks by the way, since an attacker could simply brute force the keystore itself instead of using PwMan3 to find out the correct password. The best protection is a secure password that would take a long time to guess, and to keep your keystore file safe.

#### Why is it named PwMan3? ####
PwMan is short for Password Manager. To explain the *3*, a bit of history: In 2010 I felt a need for a password manager that could be worked with using only a keyboard and without memorizing lots of shortcuts. Thus a tool emerged and it was titled PwMan in lack of a better name. That tool had a very basic hard coded encryption and lacked many features such as import/export or erasing the clipboard. About 2014 I felt continueing to use PwMan was not appropriate anymore given that I started using it for work related passwords. I did some research how to implement a better encryption and decided to go with javax.crypto.Cipher and a KeyStore. Completely rewritten PwMan2 was born. It served me well over the years but it had a few minor bugs I never fixed and the code base was not as well structured as I wanted it to be. So in early 2020 I decided it should get a complete rewrite again. PwMan3 comes with a configuration file, a routine to automatically set up the keystore, all known bugs fixed, a healthy architecture with over 90% test coverage and an automated release process supported by quality assurance tools.

#### Can I run multiple instances of PwMan3? ####
Of course! For example I use separate password databases for personal and occupational passwords. You can run them in different terminal windows at the same time and the instances won't be aware of each other.

#### Can you add a graphical interface? ####
No. The core characteristic of PwMan3 is its gui-less nature. If you want a gui, use one of the popular gui-based password managers such as Keepass.

#### Can you implement feature xyz? ####
It depends. If I think that feature is useful and it fits with PwMan3 I might implement it. Why don't you open a [Gitlab Issue](https://gitlab.com/christianpflugradt/pwman3/-/issues) for it? :-)
