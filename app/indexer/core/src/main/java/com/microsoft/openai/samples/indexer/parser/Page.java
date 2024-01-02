package com.microsoft.openai.samples.indexer.parser;

public class Page {
            /**
             * A single page from a pdf
             *
             * Attributes:
             *     page_num (int): Page number
             *     offset (int): If the text of the entire PDF was concatenated into a single string, the index of the first character on the page. For example, if page 1 had the text "hello" and page 2 had the text "world", the offset of page 2 is 5 ("hellow")
             *     text (str): The text of the page
             */

            private int page_num;
            private int offset;
            private String text;

            public Page(int page_num, int offset, String text) {
                this.page_num = page_num;
                this.offset = offset;
                this.text = text;
            }

            public int getPageNum() {
                return page_num;
            }

            public int getOffset() {
                return offset;
            }

            public String getText() {
                return text;
            }
        }
