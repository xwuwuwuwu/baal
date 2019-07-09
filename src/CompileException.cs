using System;
using System.Collections.Generic;
using System.Text;

namespace AzureBootloaderCompiler
{
    public class CompileException : Exception
    {

        public CompileException() { }

        public CompileException(string message) : base(message) { }

        public CompileException(string message, Exception innerException) : base(message, innerException) { }
    }
}
