# Click & Drop Print Helper

ClickDropPrintHelper is a Java application that processes PDF files to create proof of postage documents, packing slips, and labels. It uses Apache PDFBox for PDF manipulation and Apache Commons IO for file operations.

![Bulk Proof of Postage](bulkpop.jpg)

## Features

- Create a bulk proof of postage documents from the downloaded shipping labels pdf
- Add QR codes to proof of postage documents
- Generates packing slips with custom headers and footers
- Generates a labels only pdf from the original PDF

## Requirements

- Java 11 or higher
- Maven
- Have an account with Royal Mail Click & Drop

## Click & Drop settings
The label format will need to be set as:
- Choose printout format: Separate label & dispatch note
- Choose label format: A4
- Choose dispatch note format: A4
- Choose additional options: Un-tick 'Generate custom declarations with orders' & 'Generate proof of postage with orders'
- Labels per page: 4

## Dependencies

- Apache PDFBox
- Apache Commons IO
- JAI ImageIO Core

## Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/ClickDropPrintHelper.git
    cd ClickDropPrintHelper
    ```

2. Build the project using Maven:
    ```sh
    mvn clean install
    ```

## Configuration

Create a `config.properties` file in the root directory with the following properties.  This will be generated automatically on first run if it does nto exist.

```properties
# Folder to monitor for new PDF files, usually your downloads folder
WatchFolder=path/to/watch/folder

# Folder to for storing the generated pdfs, this can also be the downloads folder or an archive folder
StoreFolder=path/to/store/folder

# Whether to create proof of postage documents (yes/no)
CreateProofOfPostage=yes

# Whether to add QR codes to proof of postage documents (yes/no)
CreateQRs=yes

# Whether to create labels seperate labels pdf (yes/no)
CreateLabels=yes

# Whether to create seperate packing slips pdf (yes/no)
CreatePackingSlips=yes

# Path to the header image for packing slips (optional)
PackingSlipHeaderImage=path/to/header/image

# Path to the footer image for packing slips (optional)
PackingSlipFooterImage=path/to/footer/image

# Whether to stop watching the folder after the first run (yes/no)
StopWatchAfterFirstRun=no