DRIVE UPLOADER

Uses the example Google Drive api code to create
an application that uploads all files from a specified local
folder to a specified remote Google Drive folder. 

The application continually watches the specified local folder for new files. Files that
are uploaded to google drive are thereafter deleted from local storage.

The application posts a notification using a slack hook upon a successful upload. The
notification includes a link for the uploaded file.

Mime types are resolved using the Java FileTypeMap.

Your Google Drive must have an OAuth key created. The application
then uses the provided credentials.json to authorize with your Google Drive. 
Your browser will open and a prompt for authorization will open.
You can find more info about creating the auth credentials in Google's
official docs.
