@Parser(
    className = "yajco.parser.Parser",
    mainNode = "yajco.model.Language",
    tokens = {
        @TokenDef(name = "BOOLEAN", regexp = "boolean"),
        @TokenDef(name = "INTEGER", regexp = "int"),
        @TokenDef(name = "REAL", regexp = "real"),
        @TokenDef(name = "STRING", regexp = "string"),

        @TokenDef(name = "NAME", regexp = "(?:[a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)|\\[([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\]"),

        @TokenDef(name = "BOOLEAN_VALUE", regexp = "true|false"),
        @TokenDef(name = "REAL_VALUE", regexp = "[-]?[0-9]+[.][0-9]+((e|E)[0-9]+)?"),
        @TokenDef(name = "STRING_VALUE", regexp = "\"([^\"]*)\""),
        @TokenDef(name = "INT_VALUE", regexp = "[0-9]+")
    },
    skips = {
        @Skip(" "),
        @Skip("\\t"),
        @Skip("\\n"),
        @Skip("\\r"),
        @Skip("//.*")
    }
)
package yajco.model;

import tuke.pargen.annotation.config.Parser;
import tuke.pargen.annotation.config.TokenDef;
import tuke.pargen.annotation.config.Skip;
