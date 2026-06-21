const { DatabaseSync } = require('node:sqlite');
const db = new DatabaseSync('playstation_database');

function query(sql) {
    console.log(`\nExecuting: ${sql}`);
    try {
        const results = db.prepare(sql).all();
        console.table(results);
    } catch (e) {
        console.error(`Error executing ${sql}: ${e.message}`);
    }
}

console.log("--- sessions ---");
query("SELECT * FROM sessions;");

console.log("\n--- session_products ---");
query("SELECT * FROM session_products;");
