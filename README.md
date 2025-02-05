# ClickDropPrintHelper

ClickDropPrintHelper is a Java application that processes PDF files to create proof of postage documents, packing slips, and labels. It uses Apache PDFBox for PDF manipulation and Apache Commons IO for file operations.

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

Create a `config.properties` file in the root directory with the following properties:

```properties
# Folder to monitor for new PDF files
watchFolder=path/to/watch/folder

# Folder to use for processing and storing intermediate files
workingFolder=path/to/working/folder

# Whether to create proof of postage documents (yes/no)
createProofOfPostage=yes

# Whether to create