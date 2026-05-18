const fs = require('fs');
const path = require('path');

const dir = path.join(__dirname, 'app/api/goals');

function traverseAndReplace(currentPath) {
  const stat = fs.statSync(currentPath);
  if (stat.isDirectory()) {
    fs.readdirSync(currentPath).forEach(file => {
      traverseAndReplace(path.join(currentPath, file));
    });
  } else if (currentPath.endsWith('.ts')) {
    let content = fs.readFileSync(currentPath, 'utf8');
    let original = content;

    content = content.replace(/const (sessionTransaction|dbSession) = await mongoose\.startSession\(\)\n\s*(sessionTransaction|dbSession)\.startTransaction\(\)\n/g, '');
    content = content.replace(/\.session\((sessionTransaction|dbSession)\)/g, '');
    content = content.replace(/, \{ session: (sessionTransaction|dbSession) \}/g, '');
    content = content.replace(/\{ session: (sessionTransaction|dbSession) \}/g, '');
    content = content.replace(/await (sessionTransaction|dbSession)\.commitTransaction\(\)\n/g, '');
    content = content.replace(/await (sessionTransaction|dbSession)\.abortTransaction\(\)\n/g, '');
    content = content.replace(/(sessionTransaction|dbSession)\.endSession\(\)\n/g, '');
    
    // fix any stray await goal.save() if it had `{ session: ... }` inside
    content = content.replace(/await goal\.save\(\)/g, 'await goal.save()');

    if (content !== original) {
      console.log('Updated:', currentPath);
      fs.writeFileSync(currentPath, content);
    }
  }
}

traverseAndReplace(dir);
console.log('Done');
