# --- Configuration ---
# The base name of your main .tex file (without the .tex extension)
TARGET := main

# The compiler to use (pdflatex, xelatex, lualatex)
LATEX_COMPILER := pdflatex

# The bibliography tool (bibtex or biber). Use 'biber' for biblatex.
BIB_TOOL := bibtex

# Number of times to run the compiler (usually 2 or 3 is enough to resolve references)
RUNS := 2

# --- File Names ---
PDF_FILE := $(TARGET).pdf
TEX_FILES := $(wildcard *.tex)
BIB_FILES := $(wildcard *.bib)

# --- Primary Targets ---

.PHONY: all pdf view clean clean_all

# Default target: builds the PDF
all: pdf

# Target to create the final PDF
pdf: $(PDF_FILE)

# Rule for building the PDF (ensures references, TOC, and bibliography are resolved)
# This uses the standard sequence: LATEX -> BIB -> LATEX (xRUNS)
$(PDF_FILE): $(TEX_FILES) $(BIB_FILES)
	@echo "--- Starting compilation of $(TARGET).tex ---"
	# Initial run to generate aux files
	@$(LATEX_COMPILER) $(TARGET).tex
	# Run bibliography tool (only if .aux file exists)
	@if [ -f "$(TARGET).aux" ]; then \
		echo "Running $(BIB_TOOL)..."; \
		$(BIB_TOOL) $(TARGET); \
	fi
	# Final runs to resolve all references and TOC
	@echo "Running $(LATEX_COMPILER) $(RUNS) more times to resolve references..."
	@i=0; while [ $$i -lt $(RUNS) ]; do \
		$(LATEX_COMPILER) $(TARGET).tex; \
		i=$$((i+1)); \
	done
	@echo "--- Compilation successful. Created $(PDF_FILE) ---"

# Target to open the compiled PDF
view: $(PDF_FILE)
	@echo "Opening $(PDF_FILE)..."
	# Use 'open' for macOS, 'xdg-open' for Linux, 'start' for Windows (via Git Bash/Cygwin)
	@if [ "$(OS)" = "Windows_NT" ]; then start $(PDF_FILE); \
	elif [ "$(shell uname)" = "Darwin" ]; then open $(PDF_FILE); \
	else xdg-open $(PDF_FILE); fi

# Target to clean up common intermediate files
clean:
	@echo "Cleaning up intermediate files..."
	@rm -f $(TARGET).aux $(TARGET).log $(TARGET).bbl $(TARGET).blg $(TARGET).toc $(TARGET).lof $(TARGET).lot $(TARGET).out $(TARGET).dvi $(TARGET).idx $(TARGET).ilg $(TARGET).ind $(TARGET).fls $(TARGET).synctex.gz

# Target to clean all intermediate files and the final PDF
clean_all: clean
	@echo "Removing final PDF file..."
	@rm -f $(PDF_FILE)
