package com.example.notewidget.service;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.util.Arrays;

public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownService() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()
        ));

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();
    }

    public String toHtmlDocument(String markdown) {
        String source = markdown == null ? "" : markdown;
        Node document = parser.parse(source);
        String htmlBody = renderer.render(document);

        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    body {
                      margin: 0;
                      padding: 18px;
                      font-family: "Microsoft YaHei UI", "Segoe UI", sans-serif;
                      line-height: 1.65;
                      color: #1f2937;
                      background: #ffffff;
                    }
                    h1, h2, h3, h4, h5, h6 {
                      margin-top: 1.2em;
                      margin-bottom: 0.5em;
                      line-height: 1.3;
                    }
                    p { margin: 0.6em 0; }
                    code {
                      background: #f4f6f8;
                      padding: 2px 6px;
                      border-radius: 4px;
                      font-family: "Consolas", "JetBrains Mono", monospace;
                    }
                    pre {
                      background: #f8fafc;
                      padding: 12px;
                      border-radius: 8px;
                      overflow-x: auto;
                      border: 1px solid #e5e7eb;
                    }
                    blockquote {
                      margin: 0.8em 0;
                      padding: 0.3em 0.8em;
                      border-left: 4px solid #94a3b8;
                      color: #475569;
                      background: #f8fafc;
                    }
                    table {
                      border-collapse: collapse;
                      width: 100%%;
                      margin-top: 0.8em;
                    }
                    th, td {
                      border: 1px solid #d1d5db;
                      padding: 8px;
                      text-align: left;
                    }
                    th { background: #f3f4f6; }
                    img {
                      max-width: 100%%;
                      height: auto;
                    }
                  </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(htmlBody);
    }
}
