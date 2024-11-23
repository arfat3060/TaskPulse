using Microsoft.AspNetCore.Mvc;
using TaskPulse.Interfaces;

namespace TaskPulse.Controllers;

[ApiController]
[Route("api/[controller]")]
public class TaskUserController : Controller
{
    
    private readonly IUserRepository _userRepository;
    private readonly ILogger<TaskUserController> _logger;

    public TaskUserController(IUserRepository userRepository, ILogger<TaskUserController> logger)
    {
        _userRepository = userRepository;
        _logger = logger;
    }

    [HttpGet("UserData/{userID}")]
    public async Task<IActionResult> GetUserData()
    {
        try
        {
            var result = await _userRepository.GetAllUserDataAsync();
            if(result==null)
                return NotFound();
            return Ok(result);
        }
        catch (Exception e)
        {
            _logger.LogError(e, e.Message);
            throw;
        }
    }
}