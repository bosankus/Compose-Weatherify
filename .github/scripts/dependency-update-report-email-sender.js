module.exports = ({ }) => {
    const execSync = require('child_process').execSync
    execSync(`npm install nodemailer`) // Install nodemailer
    const nodemailer = require('nodemailer')
    const transporter = nodemailer.createTransport({
        host: "smtpout.secureserver.net",
        port: 587,
        auth: {
            user: `${process.env.MAIL_USERNAME}`, // I am using hotmail. You can use gmail, yandex etc.
            pass: `${process.env.MAIL_PASSWORD}` // You can use token too. I use mail and password
        }
    });
    const report = require('fs').readFileSync('build/dependencyUpdates/dependency_update_report.txt', 'utf8')

    const mailOptions = {
        from: {
            name: 'Weatherify-MVVM',
            address: process.env.MAIL_USERNAME
        },
        to: 'ankushbose5@gmail.com', // Use your main account to get the email
        subject: 'Weatherify MVVM - Dependency update report',
        text: `${report}`
    };

    transporter.sendMail(mailOptions).then(() => {
              console.log('Email sent successfully');
           }).catch((err) => {
              console.log('Failed to send email');
              console.error(err);
           });
}