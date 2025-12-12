\NeedsTeXFormat{LaTeX2e}
\ProvidesClass{app_report}[2025/12/05 APP Final Project Report Class]

% Load the base class
\LoadClass[12pt]{article}

% --- PACKAGES ---
\RequirePackage{graphicx} 
\RequirePackage{fancyhdr}
\RequirePackage[a4paper,left=2.2cm,top=3.65cm,right=2.2cm,bottom=2.2cm,headheight=1.6cm]{geometry}
\RequirePackage{lastpage}
\RequirePackage{ulem}
\RequirePackage{titlesec}
\RequirePackage{xcolor}
\RequirePackage{amssymb} % For checkboxes
\RequirePackage{enumitem} 
\RequirePackage[hidelinks]{hyperref}
\RequirePackage{etoolbox} 

% --- FONT SETUP ---
\RequirePackage[T1]{fontenc}
\RequirePackage[scaled]{helvet}
\renewcommand\familydefault{\sfdefault} 

% --- HEADER/FOOTER SETUP ---
\fancypagestyle{firststyle}{
    \fancyhead{}
    \fancyhead[C]{\includegraphics[width=\textwidth]{images/USI Logo Informatics.pdf}} 
    \renewcommand{\headrulewidth}{0pt} 
    \fancyfoot[L]{\includegraphics[height=0.5cm]{images/swissuniversity.pdf}}
    \fancyfoot[R]{\footnotesize Page \thepage/\pageref*{LastPage}}
}

\fancypagestyle{generalstyle}{
    \pagestyle{fancy}
    \fancyhf{}
    \fancyhead[L]{\includegraphics[width=\textwidth]{images/usi logo short.pdf}}
    \renewcommand{\headrulewidth}{0pt} 
    \fancyfoot[L]{\includegraphics[height=0.5cm]{images/swissuniversity.pdf}}
    \fancyfoot[R]{\footnotesize Page \thepage/\pageref*{LastPage}}
}

\pagestyle{generalstyle}
\setlength{\parindent}{0pt}

% --- CUSTOM COMMANDS FOR STUDENTS ---

% 1. CHECKBOXES
\newcommand{\unchecked}{\raisebox{-.2ex}{\Large $\square$}}
\newcommand{\checked}{\raisebox{-.2ex}{\Large $\boxtimes$}}

% 2. BOX SIZES (Fixed dimensions)
% Internal command for the box logic (Renamed to avoid @ symbol issues)
\newcommand{\MakeAnswerBoxInternal}[2]{%
    \vspace{0.2cm}
    \noindent\fbox{%
        \begin{minipage}[t][#1]{\dimexpr\textwidth-2\fboxsep-2\fboxrule\relax}
            \raggedright
            #2
        \end{minipage}%
    }
    \vspace{0.5cm}
}

% Public commands for students to use
\newcommand{\AnswerBoxTiny}[1]{\MakeAnswerBoxInternal{1cm}{#1}}    % For names/single lines
\newcommand{\AnswerBoxSmall}[1]{\MakeAnswerBoxInternal{2cm}{#1}}   % Max 200 chars
\newcommand{\AnswerBoxMedium}[1]{\MakeAnswerBoxInternal{3cm}{#1}}  % Max 300 chars
\newcommand{\AnswerBoxLarge}[1]{\MakeAnswerBoxInternal{5cm}{#1}}   % Max 650 chars (compact)
\newcommand{\AnswerBoxXLarge}[1]{\MakeAnswerBoxInternal{6cm}{#1}}  % Max 650 chars (standard)
\newcommand{\AnswerBoxHuge}[1]{\MakeAnswerBoxInternal{16cm}{#1}}   % Max 1 page

% 3. GROUP INFO COMMANDS
% Using safe variable names
\newcommand{\groupname}[1]{\def\TheGroupName{#1}}
\newcommand{\groupmembers}[1]{\def\TheGroupMembers{#1}}

% Initialize empty to prevent errors if not set
\def\TheGroupName{}
\def\TheGroupMembers{}

% 4. TITLE PAGE GENERATOR
\newcommand{\makeformtitle}{
    \thispagestyle{firststyle}
    \vspace*{0.5cm}
    \noindent
    {\Large \textbf{Lecture Mobile and Wearable Computing, fall term 2025}} \\[0.2cm]
    {\Large \textbf{Android Programming Project (APP) - Final project report}}
    \vspace{1cm}
    
    {\large
    \textbf{Group name:} \underline{\smash{\makebox[10cm][l]{\TheGroupName}}} \\[0.5cm]
    \textbf{Group members:} \underline{\smash{\makebox[10cm][l]{\TheGroupMembers}}}
    }
    \vspace{1cm}
    
    \section*{Instructions}
    \begin{itemize}
        \item Please answer the questions reported in the following pages using the foreseen text boxes. 
        \item Do not change the format, font size or any other elements of this template.
        \item \textbf{Be concise and precise.}
        \item Do not exceed the given limit of maximal number of characters.
        \item "The app" mentioned in the questions refers to the Android-based application that you develop in the context of the Mobile and Wearable Computing class.
        \item Once compiled, please upload the document on iCorsi.
    \end{itemize}

    \vspace{0.5cm}
    \textbf{The deadline for sending the document is Monday December 15, 2024, 23:59 CEST}.

    If you have questions: post your question(s) in the iCorsi forum or e-mail \href{mailto:ardian.selmonaj@usi.ch}{ardian.selmonaj@usi.ch} and \href{mailto:leonardo.alchieri@usi.ch}{leonardo.alchieri@usi.ch}.
    \clearpage
}