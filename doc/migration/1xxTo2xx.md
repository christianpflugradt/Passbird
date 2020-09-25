# PwMan3 version 1.x.x to version 2.x.x migration guide #

## breaking changes ##

PwMan3 version 2 introduces two breaking change:

 * Password aliases may no longer contain special characters. They should only contain alphabetic characters and digits.
 * Password aliases may no longer start with a digit. The first letter of a password alias must be an alphabetic character.

This breaking change is a preparation for future features enhancing existing commands and introducing new ones. Limiting future commands to single alphabetic characters would lead to non intuitive commands and an overall limit of 26 commands, so in favor of comprehensible commands this constraint was introduced.

## is migration required? ##

Short answer: Yes, you should. If you know you don't have any invalid aliases in your database you can skip step 3 and migration will only take you a minute.

Long answer: Yes and no. To reflect the breaking change the signature of the password database has changed. By default the configuration property *verifySignature* is set to *true* and PwMan3 will refuse to start if the signature deviates from the expectation. You can however set that property to *false* and PwMan3 will start nonetheless. It will indicate the mismatch as an error message on each startup though, until you create/update/delete a password which will also update the signature. The reason for this behaviour is that PwMan3 expects the password database to be valid and will not validate its content, thus doing that with a password database that contain aliases with digits and special characters, which are no longer supported from version 2 on, will possibly break these entries in the future. So unless you know what you are doing, better follow the migration guide.

## how to migrate: quick quide #

This quick guide is for experienced users. If you have troubles, follow the in-depth migration guide below.

 * update PwMan3 to the latest 1.x.x version first
 * export your password database
 * edit the export file replacing all invalid aliases with valid ones
 * backup your database and executable file; also delete or rename the original database file so PwMan3 will not detect in on next start
 * update to the latest PwMan3 2.x.x and run PwMan3 => it should create a new, empty database file
 * import the json file you exported earlier => use the list command to verify that all password entries have been imported

## how to migrate: in-depth ##

Migration can be done within a few minutes unless you have a large password database with many incompatible aliases.

1) At first make sure you have the latest PwMan3 1.x.x version installed which is PwMan3 1.4.2. Upgrading from an earlier version directly to PwMan3 version 2 is not recommended and the migration guide might not work for you.

2) Export your PwMan3 version 1 password database using the export command. For example to export your database into the /tmp directory which is a valid directory on Linux and MacOS, enter the following command in PwMan3: *e/tmp*

This will create a file */tmp/PwMan3.json*. 

3) Edit this text file with a text editor of your choice and replace all aliases that start with digits or contain special characters with alphabetic characters and digits only after the first letter. Each alias must remain unique. Special characters include braces, underscores, punctuation marks, currency symbols and others. Now save your changes to the text file while keeping the file name (don't rename it to another file extension like '.txt').

The structure of this file is quite simple: For each password entry stored in your database you will find a pair of strings that looks like this: *"key":"value"*

The placeholder *value* will be the password alias. Say you have an alias named *1login* the entry will look like this: *"key":"1login"*

Now you know PwMan3 will no longer support a digit as the first character and generally no special characters in aliases, so you must rename this alias. Let's say you want to rename this alias to *login1*. The result should then be as follows: *"key":"login1"*

It is important that you do not modify the structure of the file. Only edit the alias enclosed in quotes. If you edit other elements or remove a quote, PwMan3 might not be able to import the file correctly later.

4) Once you are done rename your database file named *PwMan3.db* to *PwMan3.db.old* and your PwMan3 executable file which is probably named *pwman3.jar* to *pwman3.jar.old*.

5) Now upgrade to PwMan3 version 2 as you would usually update to another version (download the jar file, put it in your pwman3 directory, rename it if you want). You must at least upgrade do PwMan3 2.1.0 as previous versions where more strict with digits, but I recommend to upgrade straight to the latest 2.x.x version.

6) Run PwMan3 version 2 and it will auto-create an empty database file. PwMan3 will not start in setup mode because it will detect your keystore file *PwMan3.ks*. Now import the file *PwMan3.json* that you created earlier using the export command. If you removed every special character from your aliases and every digit from the first position of your aliases, import will be successfully. You can use the *list* command to check if all password entries have been imported. Note that you would also get an error message if import was unsuccessful.