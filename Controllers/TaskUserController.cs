using Microsoft.AspNetCore.Mvc;

namespace TaskPulse.Controllers;

public class TaskUserController : Controller
{
    // GET
    public IActionResult Index()
    {
        return View();
    }
}